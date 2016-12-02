# Fun.CQRS Workshop

This workshop is designed to give developers the necessary skills to start building CQRS and Event Sourced applications using Fun.CQRS.

You will learn the basic principles of CQRS / ES and how to model a domain in terms of Commands and Events. We will explore some strategies that can be applied when designing aggregates (write-models) and views (read-models).

If time allows we will have a deep dive into Fun.CQRS internals and explore some design choices we have made while building its interpreter and its different backends.


## Introduction

This is a classical shopping cart application.

During the workshop we will implement the `Order` behavior (write-side) and the `OrderDetails` Projection (read-side). 

This will be done initially using only unit tests and the `InMemoryBackend`. Once everything is in place we will run the Play server and see it in action using the `AkkaBackend`.

We will also see how to deal with remote services inside the aggregate's behavior. We have three 'fake remote services': `StockService`, `BillingService` and `ProductService`.


### Setup

Check out this project

```
git clone git@github.com:strongtyped/fun-cqrs-order-demo.git
cd fun-cqrs-scalaio

# start sbt and call `groll initial` to bring the repo to it's initial state
sbt
groll initial
```


### Use Cases
 - User can open an order and add items to it
 - User can cancel an order
 - User can execute an order (pay it)
 - Payed or Cancelled orders reach the end-of-life and therefore cannot accept any subsequent commands

An Order is modelled as an Algebraic Data Type as are its `Commands` and `Events`. 

An order has four possible states:  

 - **EmptyOrder** - first state once we initialize it. It has not items.  
Accepts **AddItem** and **CancelOrder** commands.

 - **NonEmptyOrder** - a non-empty order has at least one item.    
Accepts **AddItem**, **RemoveItem**, **PayOrder** and **CancelOrder** commands.  
If all items are removed, **NonEmptyOrder** transition back to **EmptyOrder** state.

- **PayedOrder**   
A payed order cannot accept any new commands and therefore is considered to have reached its end-of-life.

- **CancelledOrder**  
A cancelled order cannot accept any new commands and therefore is considered to have reached its end-of-life.

An **Order** has the following **Protocol** expressed as `Commands` and `Events`.

##### Commands
```scala
case object CreateOrder extends OrderCommand
case class AddItem(itemId: ItemId, name: String, price: Double) extends OrderCommand
case class RemoveItem(itemId: ItemId) extends OrderCommand
case object CancelOrder extends OrderCommand
case class PayOrder(accountNumber: AccountNumber) extends OrderCommand
```
##### Events
```scala
case class OrderWasCreated(number: OrderNumber) extends OrderEvent
case class ItemWasAdded(number: OrderNumber, itemId: ItemId, name: String, price: Double) extends OrderEvent

case class ItemWasRemoved(number: OrderNumber, itemId: ItemId) extends OrderEvent
case class OrderWasCancelled(number: OrderNumber) extends OrderEvent
case class OrderWasPayed(number: OrderNumber, accountNumber: AccountNumber) extends OrderEvent
```


### Interacting with the webapp

There is no GUI, only REST interface. Any REST client will do the trick. The calls are extremely simple. 

If your OS offers a shell environment, we recommend to install [HTTPie](https://httpie.org/) and use the provided `api.sh`

```bash
cd fun-cqrs-order-demo
. api.sh
# to see all available calls
order.<tab> 
list.<tab>
```

### API Calls Play 
```shell
# COMMAND SIDE
# create an order
PUT http://localhost:9000/order

# add an item
POST http://localhost:9000/order/{order_id}/items
# use payload = { "value": "{item_id}" }
# item_id can be any string starting with 00, any other ID is out-of-stock

# remove an item
DELETE http://localhost:9000/order/{order_id}/items/{item_id}

# cancel an order
DELETE http://localhost:9000/order/{order_id}

# execute the order (pay)
POST http://localhost:9000/order/{order_id}
# use payload = { "value": "{account_number}" }
# account_number can be whatever you want. If it starts with 'no-saldo' payment will be refused.
#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


# QUERY SIDE 
GET http://localhost:9000/order/{order_id}
GET http://localhost:9000/orders/payed
GET http://localhost:9000/orders/cancelled
GET http://localhost:9000/orders/open
#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

```
