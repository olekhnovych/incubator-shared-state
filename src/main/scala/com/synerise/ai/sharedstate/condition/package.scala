package com.synerise.ai.sharedstate


package object condition {
  case class True() extends Condition {
    def apply(sharedState: SharedState): Boolean = true

    override def toString: String = "true"
  }

  case class And(conditions: Condition*) extends Condition {
    def apply(sharedState: SharedState): Boolean =
      conditions.map(_(sharedState)).foldLeft(true)(_ && _)

    override def toString: String = s"(${conditions.mkString(" AND ")})"
  }

  case class Or(conditions: Condition*) extends Condition {
    def apply(sharedState: SharedState): Boolean =
      conditions.map(_(sharedState)).foldLeft(false)(_ || _)

    override def toString: String = s"(${conditions.mkString(" OR ")})"
  }

  case class FieldEquals(fieldName: String, value: String) extends Condition {
    def apply(sharedState: SharedState): Boolean =
      sharedState.fields.get(fieldName).map(_ == value).getOrElse(false)

    override def toString: String = s"(${fieldName} == '${value})'"
  }
}
