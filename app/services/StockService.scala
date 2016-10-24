package services

import model.ItemId
import scala.concurrent.Future

trait StockService {
  def reserveItem(productIds: ItemId*): Future[Done]
  def unreserveItem(productIds: ItemId*): Future[Done]
}
