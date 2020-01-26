package com.synerise.ai.sharedstate


case class SharedState(fields: Map[String, String], keyFields: Set[String], version: Long) {
  def key = fields.filterKeys(keyFields)
  def entry = key -> this
  def updateVersion = SharedState(fields, keyFields, version+1)
}


object SharedState {
  def apply(fields: Map[String, String], keyFields: Set[String]): SharedState = SharedState(fields, keyFields, 0)
}
