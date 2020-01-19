package com.synerise.ai.sharedstate.storagebackend

import com.synerise.ai.sharedstate._


case class MemoryStorageBackend(sharedStates: Map[SharedStateKey, SharedState]) extends StorageBackend {
  def update(sharedState: SharedState) =
    MemoryStorageBackend(sharedStates + sharedState.entry)

  def fetch(condition: Condition) =
    sharedStates.values.filter(condition).toList
}

object MemoryStorageBackend {
  def apply() = new MemoryStorageBackend(Map.empty)
}
