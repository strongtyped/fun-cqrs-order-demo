package model

import play.api.libs.json.Json

case class ItemId(value: String)

object ItemId {
  implicit val format = Json.format[ItemId]
}
