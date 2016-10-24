package backend

import util.Lists
import model.read._
import model.write.OrderProtocol._

class OrderDetailsProjection(orderDetailsRepo: OrderDetailsRepo) extends SyncProjection {

  def handleEventSync: HandleEventSync = PartialFunction.empty
  
}
