package wiring

import com.softwaremill.macwire._
import controllers.orders.{ OrderCommandController, OrderQueryController }

trait OrderControllersComponent extends OrderComponent {

  lazy val orderController      = wire[OrderCommandController]
  lazy val orderQueryController = wire[OrderQueryController]

}
