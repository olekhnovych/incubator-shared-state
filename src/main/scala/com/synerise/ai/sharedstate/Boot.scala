package com.synerise.ai.sharedstate

import akka.actor.testkit.typed.scaladsl.LogCapturing
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.LoggerOps
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.AskPattern._
import scala.util.{Try, Success, Failure}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}


case class StorageImpl(sharedStates: Map[SharedStateKey, SharedState]) {
  def update(sharedState: SharedState) =
    StorageImpl(sharedStates + sharedState.entry)

  def fetch(condition: Condition) =
    sharedStates.values.filter(condition).toList
}

object StorageImpl {
  def apply() = new StorageImpl(Map.empty)
}

object Storage {
  trait Message
  case class Update(sharedState: SharedState) extends Message
  case class Fetch(condition: Condition, replyTo: ActorRef[SharedStates]) extends Message
  case class Subscribe(condition: Condition, subscriber: ActorRef[SharedStates]) extends Message
  case class Print() extends Message

  type Ref = ActorRef[Message]

  type Subscriber = ActorRef[SharedStates]
  type Subscriptions = Map[Subscriber, Condition]

  def apply(): Behavior[Message] =
    next(StorageImpl(), Map.empty)

  def next(storage: StorageImpl, subscriptions: Subscriptions): Behavior[Message] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case Update(sharedState) => {
        val (subscribers, conditions) = subscriptions.unzip

        val fetchesBefore = conditions.map(storage.fetch).toList
        val newStorage = storage.update(sharedState)
        val fetchesAfter = conditions.map(newStorage.fetch).toList

        val changedSubscriptions = (subscribers, fetchesBefore, fetchesAfter).zipped
          .collect{case (subscriber, fetchBefore, fetchAfter) if fetchBefore != fetchAfter => (subscriber, fetchAfter) }

        for((subscriber, sharedStates) <- changedSubscriptions)
          subscriber ! sharedStates

        next(newStorage, subscriptions)
      }

      case Fetch(condition, replyTo) => {
        replyTo ! storage.fetch(condition)
        next(storage, subscriptions)
      }

      case Subscribe(condition, subscriber) => {
        val newSubscriptions = subscriptions + (subscriber -> condition)
        subscriber ! storage.fetch(condition)
        next(storage, newSubscriptions)
      }

      case Print() => {
        val sharedStates = storage.fetch(condition.True())
        for (sharedState <- sharedStates)
          context.log.info(s"$sharedState")
        Behaviors.same
      }
    }
  }
}

object SharedStateFactory {
  def requiredEnabledService(owner: String, requiredServiceName: String, enabled: Boolean=true) =
    SharedState(Map("type"->"requiredEnabled",
                    "owner"->owner,
                    "serviceName" -> requiredServiceName,
                    "requiredEnabled" -> enabled.toString),
                Set("owner", "serviceName", "type"))
}


object Service {
  trait Message
  case class SharedStatesResponce(sharedStates: SharedStates) extends Message
  case class Print() extends Message

  def apply(storage: Storage.Ref, serviceName: String, requiredServiceNames: List[String]) = new Service(storage, serviceName, requiredServiceNames)()
}

class Service(val storage: Storage.Ref, serviceName: String, requiredServiceNames: List[String]) {
  def apply(): Behavior[Service.Message] = Behaviors.setup { context =>

    val sharedStatesWrapper: ActorRef[SharedStates] =
      context.messageAdapter(sharedStates => Service.SharedStatesResponce(sharedStates))

    storage ! Storage.Subscribe(condition.And(condition.FieldEquals("type", "requiredEnabled"),
                                              condition.FieldEquals("serviceName", serviceName),
                                              condition.FieldEquals("requiredEnabled", "true")), sharedStatesWrapper)

    lazy val loop: Boolean => Behavior[Service.Message] = enabled => Behaviors.receiveMessage {
      case Service.SharedStatesResponce(sharedStates) => {

        val enabled = !sharedStates.isEmpty

        for(requiredServiceName <- requiredServiceNames)
          storage ! Storage.Update(SharedStateFactory.requiredEnabledService(serviceName, requiredServiceName, enabled))

        loop(enabled)
      }
      case Service.Print() => {
        context.log.info(s"enabled: $enabled")
        Behaviors.same
      }
    }

    loop(false)
  }
}


object Main {
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

  def spawn[U](behavior: Behavior[U], name: String)(implicit actorSystem: ActorSystem[Main.Message]) = {
    implicit val timeout: Timeout = 3.seconds
    implicit val executionContext = actorSystem.executionContext

    val responseFuture = actorSystem.ask((ref: ActorRef[Main.SpawnResponse[U]]) => Main.Spawn(behavior, name, ref))
    Await.result(responseFuture, 3.seconds).actorRef
  }
}


object Boot extends App {
  implicit val system: ActorSystem[Main.Message] =
    ActorSystem(Main(), "main")

  val storage = Main.spawn(Storage(), "storage")
  val serviceA = Main.spawn(Service(storage, "serviceA", List("serviceC")), "serviceA")
  val serviceB = Main.spawn(Service(storage, "serviceB", List("serviceC")), "serviceB")
  val serviceC = Main.spawn(Service(storage, "serviceC", List.empty),       "serviceC")

  def print() = {
    Thread.sleep(20)
    storage ! Storage.Print()
    Thread.sleep(20)
    serviceA ! Service.Print()
    Thread.sleep(20)
    serviceB ! Service.Print()
    Thread.sleep(20)
    serviceC ! Service.Print()
  }

  print()

  storage ! Storage.Update(SharedStateFactory.requiredEnabledService("main", "serviceA", true))
  print()

  storage ! Storage.Update(SharedStateFactory.requiredEnabledService("main", "serviceB", true))
  print()

  storage ! Storage.Update(SharedStateFactory.requiredEnabledService("main", "serviceA", false))
  print()

  storage ! Storage.Update(SharedStateFactory.requiredEnabledService("main", "serviceB", false))
  print()
}
