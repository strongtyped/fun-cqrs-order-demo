package wiring

import akka.actor.ActorSystem
import com.softwaremill.macwire._
import io.funcqrs.backend.QueryByTag
import io.funcqrs.config.Api._
import backend._
import model.write.Order
import services.{ BillingService, ProductService, StockService }

trait OrderComponent extends RemoteServiceComponent {

  def actorSystem: ActorSystem

  lazy val orderBackend = wire[OrderAkkaBackend]

  def stockService: StockService

  def billingService: BillingService

  def productService: ProductService

  lazy val orderDetailsRepo = wire[OrderDetailsRepo]

  // format: off
  orderBackend
  // format: on
  .configure {
    aggregate[Order] { number =>
      // behavior get's services injected
      Order.behavior(number, stockService, billingService)
    }
  }.configure {
    projection(
      query      = QueryByTag(Order.tag),
      projection = wire[OrderDetailsProjection]
    )
  }
}
