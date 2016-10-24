package controllers.orders

import backend.OrderDetailsRepo
import model.OrderNumber
import model.read._
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }

class OrderQueryController(orderDetailsRepo: OrderDetailsRepo) extends Controller {

  def view(number: String) = Action {
    orderDetailsRepo
      .find(OrderNumber(number))
      .map { order =>
        Ok(Json.toJson(order))
      }
      .getOrElse { NotFound(s"No order found for number $number") }
  }

  def allPayed() = list(_.status == Payed)

  def allCancelled() = list(_.status == Cancelled)

  def allOpen() = list(ord => ord.status == Initiated || ord.status == Open)

  private def list(predicate: OrderDetails => Boolean) = Action {
    val orders = orderDetailsRepo.findAll(predicate)
    Ok(Json.toJson(orders))
  }
}
