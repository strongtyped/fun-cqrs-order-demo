package model.write

import io.funcqrs.config.Api._
import io.funcqrs.test.InMemoryTestSupport
import io.funcqrs.test.backend.InMemoryBackend
import model.{ AccountNumber, ItemId, OrderNumber }
import services.{ BillingService, Done, StockService }
import wiring.{ InsufficientSaldoException, ItemOutOfStockException, RemoteServiceComponent }

import scala.concurrent.Future

class InMemoryOrderTestSupport extends InMemoryTestSupport {

  val number = OrderNumber.random()

  /**
    * Returns a AggregateRef for Order.
    */
  def orderRef = this.aggregateRef[Order](number)

  def configure(backend: InMemoryBackend): Unit = {
    // ---------------------------------------------
    // aggregate config - write write.model
    backend.configure {
      aggregate[Order](number => Order.behavior(number, stockService, billingService))
    }
  }

  private var failBilling = false
  private var failStock   = false

  def withFailingBilling() = {
    failBilling = true
  }

  def withFailingStock() = {
    failStock = true
  }

  val billingService: BillingService = new BillingService {

    /** Stub method
      * Always accept payments unless 'failBilling' is true
      */
    def makePayment(accountNumber: AccountNumber, identifier: String, amount: Double): Future[Done] = {
      if (failBilling)
        Future.failed(new InsufficientSaldoException)
      else
        Future.successful(Done)
    }
  }

  val stockService: StockService = new StockService {

    /** Stub method.
      * Always accept reservation unless 'failStock' is true
      */
    def reserveItem(productIds: ItemId*): Future[Done] = {
      if (failStock) Future.failed(new ItemOutOfStockException)
      else Future.successful(Done)
    }

    /** Stub method, always accept a unreserve request */
    def unreserveItem(productIds: ItemId*): Future[Done] = Future.successful(Done)
  }
}
