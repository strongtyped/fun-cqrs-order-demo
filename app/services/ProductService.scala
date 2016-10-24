package services

import model.ItemId

import scala.concurrent.Future

trait ProductService {
  def fetchProductDetails(id: ItemId): Future[ProductDetails]
}

case class ProductDetails(id: ItemId, name: String, price: Double)
