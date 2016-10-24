package model.read

import model.{ ItemId, OrderNumber }
import play.api.libs.json.{ JsString, JsValue, Json, Writes }

case class OrderDetails(number: OrderNumber, status: OrderStatus, items: List[ItemDetails] = List.empty) {

  lazy val totalAmount = items.map(_.price).sum
}

case class ItemDetails(itemId: ItemId, name: String, price: Double)
object ItemDetails {
  implicit val writes = Json.writes[ItemDetails]
}

sealed trait OrderStatus
case object Initiated extends OrderStatus
case object Open extends OrderStatus
case object Payed extends OrderStatus
case object Cancelled extends OrderStatus

object OrderStatus {
  implicit val writes = new Writes[OrderStatus] {
    def writes(o: OrderStatus): JsValue = {
      o match {
        case Initiated => JsString("initiated")
        case Open      => JsString("open")
        case Payed     => JsString("payed")
        case Cancelled => JsString("cancelled")
      }
    }
  }
}

object OrderDetails {

  val writesCaseClass = Json.writes[OrderDetails]

  implicit val writes = new Writes[OrderDetails] {
    def writes(o: OrderDetails): JsValue = {
      Json.obj("total" -> o.totalAmount) ++ writesCaseClass.writes(o)
    }
  }
}
