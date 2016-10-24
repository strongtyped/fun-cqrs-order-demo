package backend

import akka.actor.ActorSystem
import io.funcqrs.akka.EventsSourceProvider
import io.funcqrs.akka.backend.AkkaBackend
import io.funcqrs.backend.{ Query, QueryByTag }
import model.OrderNumber
import model.write.Order

class OrderAkkaBackend(val actorSystem: ActorSystem) extends AkkaBackend {

  def sourceProvider(query: Query): EventsSourceProvider =
    query match {
      case QueryByTag(tag) => new LevelDbTaggedEventsSource(tag)
    }

  def orderRef(number: OrderNumber) = this.aggregateRef[Order](number)
}
