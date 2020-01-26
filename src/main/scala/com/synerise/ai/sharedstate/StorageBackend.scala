package com.synerise.ai.sharedstate

trait StorageBackend {
  case class UpdateResult(backend: StorageBackend, accepted: Boolean)

  def update(sharedState: SharedState, updateVersion: Boolean): UpdateResult
  def fetch(condition: Condition): SharedStates
}
