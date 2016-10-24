package model

import java.util.UUID

import io.funcqrs.AggregateId
import play.api.libs.json.Json

case class OrderNumber(value: String) extends AggregateId

object OrderNumber {
  implicit val format = Json.format[OrderNumber]
  def random() = OrderNumber(UUID.randomUUID().toString)
}