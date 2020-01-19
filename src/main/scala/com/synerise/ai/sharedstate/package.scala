package com.synerise.ai

package object sharedstate {
  type SharedStateKey = Map[String, String]
  type Condition = SharedState => Boolean
  type SharedStates = List[SharedState]
}
