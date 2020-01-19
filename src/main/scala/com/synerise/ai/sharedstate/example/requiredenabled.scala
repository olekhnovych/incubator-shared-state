package com.synerise.ai.sharedstate.examples.requiredenabled

import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import com.synerise.ai.sharedstate._
import com.synerise.ai.sharedstate.condition._


object SharedStateFactory {
  def requiredEnabledService(owner: String, requiredServiceName: String, enabled: Boolean=true) =
    SharedState(Map("type"->"requiredEnabled",
                    "owner"->owner,
                    "serviceName"->requiredServiceName,
                    "requiredEnabled"->enabled.toString),
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

    storage ! Storage.Subscribe(And(FieldEquals("type", "requiredEnabled"),
                                    FieldEquals("serviceName", serviceName),
                                    FieldEquals("requiredEnabled", "true")), sharedStatesWrapper)

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


object Run extends App {
  implicit val system: ActorSystem[Spawner.Message] =
    ActorSystem(Spawner(), "main")

  val storage = Spawner.spawn(Storage(), "storage")
  val serviceA = Spawner.spawn(Service(storage, "serviceA", List("serviceC")), "serviceA")
  val serviceB = Spawner.spawn(Service(storage, "serviceB", List("serviceC")), "serviceB")
  val serviceC = Spawner.spawn(Service(storage, "serviceC", List.empty),       "serviceC")

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
