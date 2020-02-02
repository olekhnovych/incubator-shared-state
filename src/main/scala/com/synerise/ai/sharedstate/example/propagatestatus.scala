package com.synerise.ai.sharedstate.examples.propagatestatus

import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import com.synerise.ai.sharedstate._
import com.synerise.ai.sharedstate.condition._


object SharedStateFactory {
  def serviceStatus(owner: String, status: String) =
    SharedState(Map("type"->"serviceStatus",
                    "owner"->owner,
                    "status"->status),
                Set("owner", "type"))
}


object Service {
  trait Message
  case class SharedStatesResponse(sharedStates: SharedStates) extends Message
  case class Print() extends Message
  case class UpdateStatus(status: String) extends Message

  def apply(storage: Storage.Ref, serviceName: String, watchedServiceNames: List[String]) =
    new Service(storage, serviceName, watchedServiceNames)()
}

class Service(val storage: Storage.Ref, serviceName: String, watchedServiceNames: List[String]) {
  def apply(): Behavior[Service.Message] = Behaviors.setup { context =>
    val sharedStatesWrapper: ActorRef[SharedStates] =
      context.messageAdapter(sharedStates => Service.SharedStatesResponse(sharedStates))

    storage ! Storage.Subscribe(And(FieldEquals("type", "serviceStatus"),
                                    Or(watchedServiceNames.map(watchedServiceName =>
                                         FieldEquals("owner", watchedServiceName)): _*)), sharedStatesWrapper)

    lazy val exposeStatus: (String, String) => Unit = (status, watchedStatuses) =>
      storage ! Storage.Update(SharedStateFactory.serviceStatus(serviceName, f"${status} (${watchedStatuses})"))

    lazy val loop: (String, String) => Behavior[Service.Message] =
      (status, watchedStatuses) => Behaviors.receiveMessage {
        case Service.SharedStatesResponse(sharedStates) => {
          val watchedStatuses = sharedStates.map(x => s"${x.fields("owner")}: ${x.fields("status")}").mkString(", ")

          exposeStatus(status, watchedStatuses)
          loop(status, watchedStatuses)
        }
        case Service.Print() => {
          context.log.info(s"status: $status")
          Behaviors.same
        }
        case Service.UpdateStatus(newStatus) => {
          exposeStatus(newStatus, watchedStatuses)
          loop(newStatus, watchedStatuses)
        }
      }

    exposeStatus("NOT READY", "")
    loop("NOT READY", "")
  }
}


object Run extends App {
  implicit val system: ActorSystem[Spawner.Message] =
    ActorSystem(Spawner(), "main")

  val storage = Spawner.spawn(Storage(), "storage")
  val serviceA = Spawner.spawn(Service(storage, "serviceA", List("serviceB", "serviceC")), "serviceA")
  val serviceB = Spawner.spawn(Service(storage, "serviceB", List.empty), "serviceB")
  val serviceC = Spawner.spawn(Service(storage, "serviceC", List.empty), "serviceC")

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

  serviceC ! Service.UpdateStatus("READY")
  print()
}
