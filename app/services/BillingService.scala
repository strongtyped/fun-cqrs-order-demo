package services

import model.AccountNumber

import scala.concurrent.Future

trait BillingService {
  def makePayment(accountNumber: AccountNumber, identifier: String, amount: Double): Future[Done]
}
