package model.write

import io.funcqrs.behavior._
import io.funcqrs.{ AggregateLike, CommandException, ProtocolLike, Tags }
import util.Lists
import model._
import services._

import scala.concurrent.ExecutionContext.Implicits.global

sealed trait Order extends AggregateLike {
  type Id       = OrderNumber
  type Protocol = OrderProtocol.type

  def number: OrderNumber

  def id: OrderNumber = number
}

case class EmptyOrder(number: OrderNumber) extends Order {

  import OrderProtocol._

  /**
    * Possible actions on an EmptyOrder are:
    * - add a first item
    * - cancel order
    */
  def possibleActions(stockService: StockService) = Actions.empty[Order]

}

// TODO: think about it...
// Question - why NonEmptyOrder is the only variation that has Items?
case class NonEmptyOrder(number: OrderNumber, items: List[Item] = List.empty) extends Order {

  import OrderProtocol._

  lazy val totalAmount = items.map(_.price).sum

  /**
    * Possible actions on a NonEmptyOrder are:
    * - add an item
    * - remove an item
    * - execute order
    * - cancel order
    */
  def possibleActions(stockService: StockService, billingService: BillingService) =
    Actions.empty[Order]
}

case class PayedOrder(number: OrderNumber) extends Order {
  /** end-of-life, must reject all commands */
  def rejectAllCommands = Actions.empty[Order]
}

case class CancelledOrder(number: OrderNumber) extends Order {
  /** end-of-life, must reject all commands */
  def rejectAllCommands = Actions.empty[Order]
}

object Order {

  val tag = Tags.aggregateTag("order")

  import OrderProtocol._

  /** factory actions to bootstrap aggregate */
  def factoryActions(number: OrderNumber) = Actions.empty[Order]
    
  def behavior(number: OrderNumber, stockService: StockService, billingService: BillingService): Behavior[Order] = {

    Behavior {
      // the initial behavior that triggers the creation of an order
      factoryActions(number)
    } {
      case order: EmptyOrder      => order.possibleActions(stockService)
      case order: NonEmptyOrder   => order.possibleActions(stockService, billingService)

      // game over, no more commands
      case payed: PayedOrder         => payed.rejectAllCommands
      case cancelled: CancelledOrder => cancelled.rejectAllCommands
    }
  }
}

object OrderProtocol extends ProtocolLike {

  sealed trait OrderCommand extends ProtocolCommand

  case object CreateOrder extends OrderCommand
  case class AddItem(itemId: ItemId, name: String, price: Double) extends OrderCommand
  case class RemoveItem(itemId: ItemId) extends OrderCommand

  case object CancelOrder extends OrderCommand
  case class PayOrder(accountNumber: AccountNumber) extends OrderCommand

  sealed trait OrderEvent extends ProtocolEvent {
    def number: OrderNumber
  }

  /*
    Events are all named using the simple past tense in passive voice
    (eg: OrderWasPayed), to avoid confusion with the ADT names (eg: PayedOrder).
    Otherwise we will have PayedOrder ADT and OrderPayed event!
   */
  case class OrderWasCreated(number: OrderNumber) extends OrderEvent
  case class ItemWasAdded(number: OrderNumber, itemId: ItemId, name: String, price: Double) extends OrderEvent

  case class ItemWasRemoved(number: OrderNumber, itemId: ItemId) extends OrderEvent
  case class OrderWasCancelled(number: OrderNumber) extends OrderEvent
  case class OrderWasPayed(number: OrderNumber, accountNumber: AccountNumber) extends OrderEvent
}
