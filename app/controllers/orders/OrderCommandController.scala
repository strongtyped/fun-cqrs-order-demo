package controllers.orders

import io.strongtyped.JsonReaders.StringValue
import backend.OrderAkkaBackend
import model.write.OrderProtocol._
import model.{ AccountNumber, ItemId, OrderNumber }
import play.api.libs.json.Json
import play.api.mvc._
import services.ProductService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class OrderCommandController(orderAkkaBackend: OrderAkkaBackend, productService: ProductService) extends Controller {

  // PUT
  def createOrder = Action.async {

    val cmd         = CreateOrder
    val orderNumber = OrderNumber.random()

    sendCommand(orderNumber, cmd) {
      Created("Order created").withHeaders("Location" -> s"/order/${orderNumber.value}")
    }
  }

  // -- Add a new Item to Order
  // POST
  def addItem(orderNum: String) =
    Action.async(parse.json[ItemId]) { req =>
      // build a command from ProductDetails
      val itemId = req.body

      for {
        details <- productService.fetchProductDetails(itemId)
        cmd = AddItem(itemId, details.name, details.price)
        res <- sendCommand(OrderNumber(orderNum), cmd) {
                Ok("Item added to order")
              }
      } yield res
    }
  // --------------------------------------------------------------------------

  // -- Remove a new Item from Order
  // DELETE
  def removeItem(num: String, itemId: String) = Action.async {
    sendCommand(OrderNumber(num), RemoveItem(ItemId(itemId))) {
      Ok("Item removed from order")
    }
  }
  // --------------------------------------------------------------------------

  // -- Cancel order
  // DELETE
  def cancel(num: String) = Action.async { req =>
    sendCommand(OrderNumber(num), CancelOrder) {
      Ok("Order was cancelled")
    }
  }
  // --------------------------------------------------------------------------

  // -- Pay order
  // POST
  def pay(num: String) = Action.async(parse.json[AccountNumber]) { req =>
    sendCommand(OrderNumber(num), PayOrder(req.body)) {
      Ok("Order was payed")
    }
  }
  // --------------------------------------------------------------------------

  private def sendCommand(number: OrderNumber, cmd: OrderCommand)(response: => Result): Future[Result] = {
    (orderAkkaBackend.orderRef(number) ? cmd).map { _ =>
      response
    }.recoverWith {
      case NonFatal(ex) =>
        Future.successful {
          BadRequest(
            Json.obj(
              "error" -> ex.getMessage,
              "type" -> ex.getClass.getSimpleName
            )
          )
        }
    }
  }
}
