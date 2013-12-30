package models

import play.api.libs.json.Json

// ------------------------------------
// domain object
// ------------------------------------

case class Order(id: Int = -1, details: String, validated: Boolean = false, creditCardNumber: String)

// ------------------------------------
// domain events
// ------------------------------------

case class OrderSubmitted(order: Order)
case class OrderAccepted(order: Order)

case class CreditCardValidationRequested(order: Order)
case class CreditCardValidated(orderId: Int)
case class OrderTuple(id: Int, order: Order)
case class State(orders: List[OrderTuple], timestamp: Long)

object Formatters {
  implicit val OrderFormatter = Json.format[Order]
  implicit val OrderSubmittedFormatter = Json.format[OrderSubmitted]
  implicit val OrderAcceptedFormatter = Json.format[OrderAccepted]
  implicit val CreditCardValidationRequestedFormatter = Json.format[CreditCardValidationRequested]
  implicit val CreditCardValidatedFormatter = Json.format[CreditCardValidated]
  implicit val OrderTupleFormatter = Json.format[OrderTuple]
  implicit val StateFormatter = Json.format[State]
}