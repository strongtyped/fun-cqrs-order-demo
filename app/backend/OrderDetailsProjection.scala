package backend

import util.Lists
import model.read._
import model.write.OrderProtocol._

class OrderDetailsProjection(orderDetailsRepo: OrderDetailsRepo) extends SyncProjection {

  def handleEventSync: HandleEventSync = {
    case evt: OrderWasCreated   => created(evt)
    case evt: ItemWasAdded      => itemAdded(evt)
    case evt: ItemWasRemoved    => itemRemoved(evt)
    case evt: OrderWasCancelled => cancelled(evt)
    case evt: OrderWasPayed     => payed(evt)
  }

  def created(evt: OrderWasCreated): Unit =
    orderDetailsRepo.save(OrderDetails(evt.number, Initiated))

  def itemAdded(evt: ItemWasAdded): Unit =
    orderDetailsRepo.updateById(evt.number) { ord =>
      val updated = ItemDetails(evt.itemId, evt.name, evt.price) :: ord.items
      ord.copy(items = updated, status = Open)
    }

  def itemRemoved(evt: ItemWasRemoved): Unit =
    orderDetailsRepo.updateById(evt.number) { ord =>
      val updatedItems = Lists.removeFirst(ord.items)(_.itemId == evt.itemId)

      val status =
        if (updatedItems.isEmpty) Initiated
        else Open

      ord.copy(items = updatedItems, status = status)
    }

  def cancelled(evt: OrderWasCancelled): Unit =
    orderDetailsRepo.updateById(evt.number) { ord =>
      ord.copy(status = Cancelled)
    }

  def payed(evt: OrderWasPayed): Unit =
    orderDetailsRepo.updateById(evt.number) { ord =>
      ord.copy(status = Payed)
    }
  
}
