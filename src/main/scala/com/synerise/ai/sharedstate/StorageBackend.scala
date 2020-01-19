package com.synerise.ai.sharedstate

trait StorageBackend {
  def update(sharedState: SharedState): StorageBackend
  def fetch(condition: Condition): SharedStates
}
