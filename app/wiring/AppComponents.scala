package wiring

import com.softwaremill.macwire._
import controllers.Assets
import play.api._
import play.api.routing.Router
import router.Routes

trait AppComponents extends BuiltInComponents with OrderControllersComponent {

  lazy val assets: Assets = wire[Assets]
  lazy val prefix: String = "/"
  lazy val router: Router = wire[Routes]
}
