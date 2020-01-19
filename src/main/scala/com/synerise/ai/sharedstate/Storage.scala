package com.synerise.ai.sharedstate

import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, Behavior}

import com.synerise.ai.sharedstate.storagebackend.MemoryStorageBackend
import com.synerise.ai.sharedstate.condition._


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
    next(MemoryStorageBackend(), Map.empty)

  def next(storage: MemoryStorageBackend, subscriptions: Subscriptions): Behavior[Message] = Behaviors.setup { context =>
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
        val sharedStates = storage.fetch(True())
        for (sharedState <- sharedStates)
          context.log.info(s"$sharedState")
        Behaviors.same
      }
    }
  }
}
