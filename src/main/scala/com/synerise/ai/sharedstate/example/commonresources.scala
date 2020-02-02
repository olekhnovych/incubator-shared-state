package com.synerise.ai.sharedstate.examples.commonresources

import akka.actor.typed.scaladsl.{Behaviors, LoggerOps, ActorContext, TimerScheduler}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.AskPattern._
import scala.util.{Success, Failure}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.Random

import com.synerise.ai.sharedstate._
import com.synerise.ai.sharedstate.condition._


object Service {
  trait Message
  case class Print() extends Message
  case class SharedStatesResponse(sharedStates: SharedStates) extends Message
  case class Idle() extends Message
  case class StartProcessing(resourceName: String) extends Message
  case class StopProcessing(resourceName: String) extends Message

  def apply(storage: Storage.Ref) = Behaviors.setup[Service.Message] { context =>
    Behaviors.withTimers{ timers =>
      new Service(context, timers, storage)()
    }
  }
}

class Service(context: ActorContext[Service.Message], timers: TimerScheduler[Service.Message], storage: Storage.Ref) {
  def apply(): Behavior[Service.Message] = {
    idle()
  }

  def idle(): Behavior[Service.Message] = {
    val sharedStatesWrapper: ActorRef[SharedStates] =
      context.messageAdapter(sharedStates => Service.SharedStatesResponse(sharedStates))

    storage ! Storage.Subscribe(And(FieldEquals("type", "resource"),
                                    FieldEquals("state", "toProcess")), sharedStatesWrapper)

    Behaviors.receiveMessage {
      case Service.SharedStatesResponse(sharedStates) => {
        sharedStates.headOption match {
          case Some(sharedState) => {
            context.log.info(s"received: {$sharedState}")

            implicit val timeout: Timeout = 3.seconds
            implicit val executionContext = context.executionContext

            val resourceName = sharedState.fields("resource")

            val newSharedState = SharedState(Map("type"->"resource",
                                                 "resource"->resourceName,
                                                 "state"->"processing"),
                                             Set("type", "resource"),
                                             sharedState.version)

            context.ask(storage, replyTo => Storage.UpdateWithVersion(newSharedState, replyTo)) {
              case Success(Storage.UpdateResult(true)) => {
                context.log.info("accepted")
                Service.StartProcessing(resourceName)
              }
              case Success(Storage.UpdateResult(false)) => {
                context.log.info("rejected")
                Service.Idle()
              }
              case x => {
                context.log.info(s"$x")
                Service.Idle()
              }
            }

            processing()
          }
          case _ => Behaviors.same
        }
      }
      case _ => Behaviors.same
    }
  }

  def processing(): Behavior[Service.Message] = {
    Behaviors.receiveMessage {
      case Service.StartProcessing(resourceName) => {
        context.log.info(s"start processing, $resourceName")
        timers.startSingleTimer(Service.StopProcessing(resourceName), (Random.nextInt(2000)+1000).millisecond)
        Behaviors.same
      }
      case Service.StopProcessing(resourceName) => {
        context.log.info(s"stop processing, $resourceName")
        idle()
      }
      case Service.Idle() =>
        idle()
      case _ => Behaviors.same
    }
  }
}


object Run extends App {
  implicit val system: ActorSystem[Spawner.Message] =
    ActorSystem(Spawner(), "main")

  val storage = Spawner.spawn(Storage(), "storage")
  val serviceA = Spawner.spawn(Service(storage), "serviceA")
  val serviceB = Spawner.spawn(Service(storage), "serviceB")

  storage ! Storage.Update(SharedState(Map("type"->"resource",
                                           "resource"->"resource-a",
                                           "state"->"toProcess"),
                                       Set("type", "resource")))

  storage ! Storage.Update(SharedState(Map("type"->"resource",
                                           "resource"->"resource-b",
                                           "state"->"toProcess"),
                                       Set("type", "resource")))

  storage ! Storage.Update(SharedState(Map("type"->"resource",
                                           "resource"->"resource-c",
                                           "state"->"toProcess"),
                                       Set("type", "resource")))

  Thread.sleep(5000)
  storage ! Storage.Print()


}
