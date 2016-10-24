package wiring

import model.{ AccountNumber, ItemId }
import services._

import scala.concurrent.Future

trait RemoteServiceComponent {

  val billingService: BillingService = new BillingService {

    /** Stub method
      * All payments are accepted, except for account numbers starting with 'no-saldo'.
      */
    def makePayment(accountNumber: AccountNumber, identifier: String, amount: Double): Future[Done] = {
      if (accountNumber.value.startsWith("no-saldo"))
        Future.failed(new InsufficientSaldoException)
      else
        Future.successful(Done)
    }
  }

  val stockService: StockService = new StockService {

    /** Stub method.
      * Accept reservation for all item IDs starting with 00
      * all other value will be rejected with 'out-of-stock' error
      */
    def reserveItem(productIds: ItemId*): Future[Done] = {
      val inStock = productIds.forall(_.value.startsWith("00"))
      if (inStock) Future.successful(Done)
      else Future.failed(new ItemOutOfStockException)
    }

    /** Stub method, always accept a unreserve request */
    def unreserveItem(productIds: ItemId*): Future[Done] = Future.successful(Done)
  }

  val productService: ProductService = new ProductService {
    def fetchProductDetails(id: ItemId): Future[ProductDetails] = {
      Future.successful {
        ProductDetails(id, s"name-${id.value}", 120.0)
      }
    }
  }
}

class InsufficientSaldoException extends RuntimeException("Insufficient saldo!")
class ItemOutOfStockException extends RuntimeException("item is out-of-stock")
