package backend

import io.funcqrs.{DomainEvent, HandleEvent, Projection}

import scala.concurrent.Future

trait SyncProjection extends Projection {

  type HandleEventSync = PartialFunction[DomainEvent, Unit]

  def handleEventSync: HandleEventSync

  def handleEvent: HandleEvent = {
    case e if handleEventSync.isDefinedAt(e)=> Future.successful(handleEventSync(e))
  }

}
