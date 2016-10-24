package io.strongtyped

import play.api.libs.json.Json

object JsonReaders {

  case class StringValue(value: String) extends AnyVal
  object StringValue {
    implicit val format = Json.format[StringValue]
  }

  case class IntValue(value: Int) extends AnyVal
  object IntValue {
    implicit val format = Json.format[IntValue]
  }

  case class DoubleValue(value: Double) extends AnyVal
  object DoubleValue {
    implicit val format = Json.format[DoubleValue]
  }

}