package com.synerise.ai.sharedstate.storagebackend

import com.synerise.ai.sharedstate._


case class MemoryStorageBackend(stack: Map[SharedStateKey, SharedState]) extends StorageBackend {
  def update(sharedState: SharedState, updateVersion: Boolean) = {
    val currentSharedState = stack.get(sharedState.key) match {
      case Some(currentSharedState) if currentSharedState.version == sharedState.version =>
        Some(if (updateVersion) sharedState.updateVersion else sharedState)

      case None => Some(sharedState)
      case _ => None
    }

    currentSharedState match {
      case Some(currentSharedState) =>
        UpdateResult(MemoryStorageBackend(stack + currentSharedState.entry), true)
      case None =>
        UpdateResult(MemoryStorageBackend(stack), false)
    }
  }

  def fetch(condition: Condition) =
    stack.values.filter(condition).toList
}

object MemoryStorageBackend {
  def apply() = new MemoryStorageBackend(Map.empty)
}
