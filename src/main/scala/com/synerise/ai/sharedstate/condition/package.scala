package com.synerise.ai.sharedstate


package object condition {
  case class True() extends Condition {
    def apply(sharedState: SharedState): Boolean = true
  }

  case class And(conditions: Condition*) extends Condition {
    def apply(sharedState: SharedState): Boolean =
      conditions.map(_(sharedState)).foldLeft(true)(_ && _)
  }

  case class Or(conditions: Condition*) extends Condition {
    def apply(sharedState: SharedState): Boolean =
      conditions.map(_(sharedState)).foldLeft(false)(_ || _)
  }

  case class FieldEquals(fieldName: String, value: String) extends Condition {
    def apply(sharedState: SharedState): Boolean =
      sharedState.fields.get(fieldName).map(_ == value).getOrElse(false)
  }
}
