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
  def possibleActions(stockService: StockService) =
    addFirstItem(stockService) ++ cancel

  def addFirstItem(stockService: StockService) =
    // format: off
    actions[Order]
      .handleCommand {
        cmd: AddItem =>
          stockService.reserveItem(cmd.itemId).map { _ =>
            ItemWasAdded(number, cmd.itemId, cmd.name, cmd.price)
          }
      }
      .handleEvent { evt: ItemWasAdded =>
        val item = Item(evt.itemId, evt.name, evt.price)
        NonEmptyOrder(number, List(item))
      }
    // format: on

  def cancel =
    // format: off
    actions[Order]
      .handleCommand {
        // notice that an EmptyOrder doesn't have to cancel any reservation
        cmd: CancelOrder.type => OrderWasCancelled(number)
      }
      .handleEvent {
        evt: OrderWasCancelled => CancelledOrder(number)
      }
  // format: on

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
    addItems(stockService) ++
      removeItems ++
      pay(stockService, billingService) ++
      cancel

  /**
    * Command and Event handlers for adding an item to the [[Order]].
    * Depends on [[StockService]] to reserve Item on stock
    */
  def addItems(stockService: StockService) =
    // format: off
    actions[Order]
      .handleCommand {
        cmd: AddItem =>
          // AddItem requires a 'remote' call and returns a Future
          stockService.reserveItem(cmd.itemId).map { _ =>
            ItemWasAdded(number, cmd.itemId, cmd.name,  cmd.price)
          }
      }
      .handleEvent {
        evt: ItemWasAdded =>
          val item = Item(evt.itemId, evt.name, evt.price)
          copy(items = item :: items)
      }
    // format: on

  /**
    * Command and Event handlers for removing an item from the [[Order]].
    */
  def removeItems =
    // format: off
    actions[Order]
      .handleCommand {
        cmd: RemoveItem =>
          // TODO: what about the item reservation?
          // if there is an item for this code, emit event
          // otherwise do nothing
          items.find(_.itemId == cmd.itemId).map { _ =>
            ItemWasRemoved(number, cmd.itemId)
          }
      }
      .handleEvent {
        evt: ItemWasRemoved =>
          val updatedItems = Lists.removeFirst(items)(_.itemId == evt.itemId)

          // is order empty now? go back to start
          if(updatedItems.isEmpty) EmptyOrder(number)
          else copy(items = updatedItems)
          
      }
    // format: on

  /**
    * Command and Event handlers for removing an item from the [[Order]].
    */
  def cancel =
    // format: off
    actions[Order]
        .handleCommand {
          // TODO: what about the reservations?
          cmd: CancelOrder.type => OrderWasCancelled(number)
        }
        .handleEvent {
          evt: OrderWasCancelled => CancelledOrder(number)
        }
    // format: on

  /**
    * Command and Event handlers for paying the [[Order]].
    */
  def pay(reservationService: StockService, billingService: BillingService) =
    // format: off
    actions[Order]
      .handleCommand {
        cmd: PayOrder =>
          billingService
            .makePayment(cmd.accountNumber, number.value, totalAmount)
            .map { _ =>
              OrderWasPayed(number, cmd.accountNumber)
            }
      }
      .handleEvent {
        evt: OrderWasPayed => PayedOrder(number)
      }
    // format: on
}

case class PayedOrder(number: OrderNumber) extends Order {

  /** end-of-life, must reject all commands */
  def rejectAllCommands =
    // format: off
    actions[Order]
      .rejectCommand {
        case anyCommand => new CommandException(s"Order [${number.value}] is already payed ")
      }
    // format: on
}

case class CancelledOrder(number: OrderNumber) extends Order {

  /** end-of-life, must reject all commands */
  def rejectAllCommands =
    // format: off
    actions[Order]
        .rejectCommand {
          case anyCommand => new CommandException(s"Order [${number.value}] was cancelled ")
        }
    // format: on
}

object Order {

  val tag = Tags.aggregateTag("order")

  import OrderProtocol._

  /** factory actions to bootstrap aggregate */
  def factoryActions(number: OrderNumber, stockService: StockService) =
    // format: off
    actions[Order]
      .handleCommand {
        cmd: CreateOrder.type => OrderWasCreated(number)
      }
      .handleCommand {
        cmd: AddItem => 
          stockService.reserveItem(cmd.itemId).map { _ =>
            List(
              OrderWasCreated(number),
              ItemWasAdded(number, cmd.itemId, cmd.name, cmd.price)
            )
          }
          
      }
      .handleEvent {
        evt: OrderWasCreated => EmptyOrder(evt.number)
      }
    // format: on

  def behavior(number: OrderNumber, stockService: StockService, billingService: BillingService): Behavior[Order] = {

    Behavior {
      // the initial behavior that triggers the creation of an order
      factoryActions(number, stockService)
    } {
      case order: EmptyOrder    => order.possibleActions(stockService)
      case order: NonEmptyOrder => order.possibleActions(stockService, billingService)

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
