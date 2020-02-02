package com.synerise.ai.sharedstate

import akka.actor.typed.scaladsl.{Behaviors, LoggerOps}
import akka.actor.typed.{ActorRef, Behavior}

import com.synerise.ai.sharedstate.storagebackend.MemoryStorageBackend
import com.synerise.ai.sharedstate.condition._


object Storage {
  trait Message
  case class Update(sharedState: SharedState) extends Message
  case class UpdateWithVersion(sharedState: SharedState, replyTo: ActorRef[UpdateResult]) extends Message
  case class Fetch(condition: Condition, replyTo: ActorRef[SharedStates]) extends Message
  case class Subscribe(condition: Condition, subscriber: ActorRef[SharedStates]) extends Message
  case class Print() extends Message

  case class UpdateResult(accepted: Boolean)

  type Ref = ActorRef[Message]

  type Subscriber = ActorRef[SharedStates]
  type Subscriptions = Map[Subscriber, Condition]

  def apply(): Behavior[Message] =
    next(MemoryStorageBackend(), Map.empty)

  def next(storageBackend: StorageBackend, subscriptions: Subscriptions): Behavior[Message] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case Update(sharedState) => {
        val (subscribers, conditions) = subscriptions.unzip

        val sharedStatesBefore = conditions.map(storageBackend.fetch).toList
        val updateResult = storageBackend.update(sharedState, false)

        if (updateResult.accepted) {
          val sharedStatesAfter = conditions.map(updateResult.storageBackend.fetch).toList

          val changedSubscriptions = (subscribers, sharedStatesBefore, sharedStatesAfter).zipped
            .collect{case (subscriber, sharedStateBefore, sharedStateAfter)
                         if sharedStateBefore != sharedStateAfter => (subscriber, sharedStateAfter) }

         for((subscriber, sharedStates) <- changedSubscriptions)
            subscriber ! sharedStates
        }

        next(updateResult.storageBackend, subscriptions)
      }

      case UpdateWithVersion(sharedState, replyTo) => {
        val (subscribers, conditions) = subscriptions.unzip

        val sharedStatesBefore = conditions.map(storageBackend.fetch).toList
        val updateResult = storageBackend.update(sharedState, true)

        replyTo ! UpdateResult(updateResult.accepted)

        if (updateResult.accepted) {
          val sharedStatesAfter = conditions.map(updateResult.storageBackend.fetch).toList

          val changedSubscriptions = (subscribers, sharedStatesBefore, sharedStatesAfter).zipped
            .collect{case (subscriber, sharedStateBefore, sharedStateAfter)
                         if sharedStateBefore != sharedStateAfter => (subscriber, sharedStateAfter) }

         for((subscriber, sharedStates) <- changedSubscriptions)
            subscriber ! sharedStates
        }

        next(updateResult.storageBackend, subscriptions)
      }

      case Fetch(condition, replyTo) => {
        replyTo ! storageBackend.fetch(condition)
        next(storageBackend, subscriptions)
      }

      case Subscribe(condition, subscriber) => {
        val newSubscriptions = subscriptions + (subscriber -> condition)
        subscriber ! storageBackend.fetch(condition)
        next(storageBackend, newSubscriptions)
      }

      case Print() => {
        val sharedStates = storageBackend.fetch(True())
        for (sharedState <- sharedStates)
          context.log.info(s"$sharedState")
        Behaviors.same
      }
    }
  }
}
