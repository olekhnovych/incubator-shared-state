package com.synerise.ai.sharedstate

import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, Behavior, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._

import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._


object Spawner {
  trait Message

  case class Spawn[U](behavior: Behavior[U], name: String, replyTo: ActorRef[SpawnResponse[U]]) extends Message
  case class SpawnResponse[U](actorRef: ActorRef[U])

  def apply(): Behavior[Message] = Behaviors.setup(context =>
    Behaviors.receiveMessage {
      case Spawn(behavior, name, replyTo) => {
        val actor = context.spawn(behavior, name)
        replyTo ! SpawnResponse(actor)
        Behaviors.same
      }
    }
  )

  def spawn[U](behavior: Behavior[U], name: String)(implicit actorSystem: ActorSystem[Spawner.Message]) = {
    implicit val timeout: Timeout = 3.seconds
    implicit val executionContext = actorSystem.executionContext

    val responseFuture = actorSystem.ask((ref: ActorRef[Spawner.SpawnResponse[U]]) => Spawner.Spawn(behavior, name, ref))
    Await.result(responseFuture, 3.seconds).actorRef
  }
}
