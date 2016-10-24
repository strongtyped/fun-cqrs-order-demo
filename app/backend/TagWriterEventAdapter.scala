package backend

import akka.persistence.journal.{ Tagged, WriteEventAdapter }
import model.write.Order
import model.write.OrderProtocol.OrderEvent

class TagWriterEventAdapter extends WriteEventAdapter {

  def manifest(event: Any): String = ""

  def toJournal(event: Any): Any = {
    event match {

      // Order events get tagged with Order tag!
      case evt: OrderEvent => Tagged(evt, Set(Order.tag.value))

      // other events are not tagged
      case evt => evt
    }
  }
}
