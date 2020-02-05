package com.synerise.ai.sharedstate

trait StorageBackend {
  case class UpdateResult(storageBackend: StorageBackend, accepted: Boolean)

  def update(sharedState: SharedState, processVersion: Boolean): UpdateResult
  def fetch(condition: Condition): SharedStates
}
