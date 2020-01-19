package com.synerise.ai.sharedstate


case class SharedState(fields: Map[String, String], keyFields: Set[String]) {
  def entry: Tuple2[SharedStateKey, SharedState] =
    fields.filterKeys(keyFields) -> this
}


object SharedStateFactory {
  def requiredEnabledService(owner: String, requiredServiceName: String, enabled: Boolean=true) =
    SharedState(Map("type"->"requiredEnabled",
                    "owner"->owner,
                    "serviceName" -> requiredServiceName,
                    "requiredEnabled" -> enabled.toString),
                Set("owner", "serviceName", "type"))
}
