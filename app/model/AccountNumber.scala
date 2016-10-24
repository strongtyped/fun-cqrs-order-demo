package model

import play.api.libs.json.Json

case class AccountNumber(value: String) extends AnyVal

object AccountNumber {
  implicit val reads = Json.reads[AccountNumber]
}
