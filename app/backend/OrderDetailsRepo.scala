package backend

import model.OrderNumber
import model.read.OrderDetails

class OrderDetailsRepo extends InMemoryRepository {

  type Model      = OrderDetails
  type Identifier = OrderNumber

  protected def $id(model: OrderDetails): OrderNumber = model.number
}
