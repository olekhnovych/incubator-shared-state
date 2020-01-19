package com.synerise.ai.sharedstate


case class SharedState(fields: Map[String, String], keyFields: Set[String]) {
  def entry: Tuple2[SharedStateKey, SharedState] =
    fields.filterKeys(keyFields) -> this
}
