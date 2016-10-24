package model.write

import io.funcqrs.{ CommandException, MissingCommandHandlerException }
import model.{ AccountNumber, ItemId }
import org.scalatest.{ FlatSpec, Matchers }
import wiring.{ InsufficientSaldoException, ItemOutOfStockException }

class OrderTest extends FlatSpec with Matchers {

  import OrderProtocol._

  behavior of "An EmptyOrder"

  it should "accept AddItem command and transition to NonEmptyOrder" in new InMemoryOrderTestSupport {

    orderRef ! AddItem(ItemId("001"), "test", 200)

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectNoMoreEvents()

    // checking that order is now an 'NonEmptyOrder'
    orderRef.state() shouldBe a[NonEmptyOrder]

  }

  it should "reject Payment command" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder

    // TODO: can we have a more expressive exception?
    intercept[MissingCommandHandlerException] {
      orderRef ! PayOrder(AccountNumber("abc"))
    }

    expectEvent[OrderWasCreated]
    expectNoMoreEvents()

  }

  it should "accept Cancellation ommand" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! CancelOrder

    expectEvent[OrderWasCreated]
    expectEvent[OrderWasCancelled]
    expectNoMoreEvents()

  }
  // ========================================================================

  // ------------------------------------------------------------------------
  behavior of "A NonEmptyOrder"

  it should "accept AddItem commands" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! AddItem(ItemId("001"), "test", 200)
    orderRef.state() shouldBe a[NonEmptyOrder]
    orderRef ! AddItem(ItemId("001"), "test", 200)

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectEvent[ItemWasAdded]
    expectNoMoreEvents()

  }

  it should "reject AddItem command if item is not in stock" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! AddItem(ItemId("001"), "test", 200)
    orderRef.state() shouldBe a[NonEmptyOrder]

    // configure StockService to fail with out-of-stock exception
    withFailingStock()

    intercept[ItemOutOfStockException] {
      orderRef ! AddItem(ItemId("not-in-stock"), "test", 200)
    }

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectNoMoreEvents()

  }

  it should "accept RemoveItem command" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! AddItem(ItemId("001"), "test", 200)
    orderRef ! AddItem(ItemId("001"), "test", 200)
    orderRef ! RemoveItem(ItemId("001"))

    orderRef.state() shouldBe a[NonEmptyOrder]

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectEvent[ItemWasAdded]
    expectEvent[ItemWasRemoved]
    expectNoMoreEvents()
  }

  it should "transition back to EmptyOrder when its last item is removed" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! AddItem(ItemId("001"), "test", 200)
    orderRef.state() shouldBe a[NonEmptyOrder]

    orderRef ! RemoveItem(ItemId("001"))
    orderRef.state() shouldBe a[EmptyOrder]

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectEvent[ItemWasRemoved]
    expectNoMoreEvents()
  }

  it should "accept Payment command" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! AddItem(ItemId("001"), "test", 200)
    orderRef ! PayOrder(AccountNumber("0012-ABCD"))

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectEvent[OrderWasPayed]
    expectNoMoreEvents()

  }

  it should "reject a Payment command if saldo is insufficient" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! AddItem(ItemId("001"), "test", 200)

    // configure BillingService to fail with insufficient saldo exception
    withFailingBilling()

    intercept[InsufficientSaldoException] {
      orderRef ! PayOrder(AccountNumber("no-saldo"))
    }

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectNoMoreEvents()

  }

  it should "accept Cancellation command" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! AddItem(ItemId("001"), "test", 200)
    orderRef ! CancelOrder

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectEvent[OrderWasCancelled]
    expectNoMoreEvents()

  }
  // ========================================================================

  // ------------------------------------------------------------------------
  behavior of "A PayedOrder"

  it should "should reject AddItem commands" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! AddItem(ItemId("001"), "test", 200)
    orderRef ! PayOrder(AccountNumber("0012-ABCD"))

    intercept[CommandException] {
      orderRef ! AddItem(ItemId("001"), "test", 200)
    }

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectEvent[OrderWasPayed]
    expectNoMoreEvents()

  }

  it should "should reject RemoveItem commands" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! AddItem(ItemId("001"), "test", 200)
    orderRef ! PayOrder(AccountNumber("0012-ABCD"))

    intercept[CommandException] {
      orderRef ! RemoveItem(ItemId("001"))
    }

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectEvent[OrderWasPayed]
    expectNoMoreEvents()

  }

  it should "should reject Payment command" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! AddItem(ItemId("001"), "test", 200)
    orderRef ! PayOrder(AccountNumber("0012-ABCD"))

    intercept[CommandException] {
      orderRef ! PayOrder(AccountNumber("0012-ABCD"))
    }

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectEvent[OrderWasPayed]
    expectNoMoreEvents()

  }

  it should "should reject Cancellation command" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! AddItem(ItemId("001"), "test", 200)
    orderRef ! PayOrder(AccountNumber("0012-ABCD"))

    intercept[CommandException] {
      orderRef ! CancelOrder
    }

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectEvent[OrderWasPayed]
    expectNoMoreEvents()

  }
  // ========================================================================

  // ------------------------------------------------------------------------
  behavior of "A CancelledOrder"

  it should "should reject new items" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! CancelOrder

    intercept[CommandException] {
      orderRef ! AddItem(ItemId("001"), "test", 200)
    }

    expectEvent[OrderWasCreated]
    expectEvent[OrderWasCancelled]
    expectNoMoreEvents()

  }
  it should "should reject item removal" in new InMemoryOrderTestSupport {

    orderRef ! CreateOrder
    orderRef ! AddItem(ItemId("001"), "test", 200)
    orderRef ! CancelOrder

    intercept[CommandException] {
      orderRef ! RemoveItem(ItemId("001"))
    }

    expectEvent[OrderWasCreated]
    expectEvent[ItemWasAdded]
    expectEvent[OrderWasCancelled]
    expectNoMoreEvents()
  }

  it should "should reject payment" in new InMemoryOrderTestSupport {
    orderRef ! CreateOrder
    orderRef ! CancelOrder

    intercept[CommandException] {
      orderRef ! PayOrder(AccountNumber("0012-ABCD"))
    }

    expectEvent[OrderWasCreated]
    expectEvent[OrderWasCancelled]
    expectNoMoreEvents()
  }

  it should "should reject cancellation" in new InMemoryOrderTestSupport {
    orderRef ! CreateOrder
    orderRef ! CancelOrder

    intercept[CommandException] {
      orderRef ! CancelOrder
    }

    expectEvent[OrderWasCreated]
    expectEvent[OrderWasCancelled]
    expectNoMoreEvents()
  }

}
