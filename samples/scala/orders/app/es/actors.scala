package es

import akka.actor.{ActorRef, Actor}
import models._
import scala.concurrent.Future
import org.reactivecouchbase.eventstore.Message
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.{Json, JsObject}
import models.State
import models.CreditCardValidated
import models.OrderSubmitted
import models.CreditCardValidationRequested
import models.OrderTuple
import models.OrderAccepted

object Broadcaster {
  val (enumerator, channel) = Concurrent.broadcast[JsObject]
  def pushValidation(ccvr: CreditCardValidationRequested) = {
    channel.push(Json.obj(
      "validator" -> true,
      "id" -> ccvr.order.id,
      "creditCardNumber" -> ccvr.order.creditCardNumber
    ))
  }
  def pushValidated(oa: OrderAccepted) = {
    channel.push(Json.obj(
      "validated" -> true,
      "id" -> oa.order.id,
      "creditCardNumber" -> oa.order.creditCardNumber
    ))
  }
}

class OrderProcessor extends Actor {

  var state = State(List.empty[OrderTuple], System.currentTimeMillis())

  def receive = {
    case OrderSubmitted(order) => {
      //println(s"order submitted : $order")
      val id = state.orders.size
      val upd = order.copy(id = id)
      state = state.copy(state.orders :+ OrderTuple(id, upd))
      Bootstrap.validator forward CreditCardValidationRequested(upd)
    }
    case CreditCardValidated(orderId) => {
      //println(s"CreditCardValidated : $orderId")
      state.orders.find(_.id == orderId).foreach { order =>
        val upd = order.order.copy(validated = true)
        state = state.copy(state.orders :+ OrderTuple(orderId, upd))
        sender ! upd
        Bootstrap.ordersHandler ! OrderAccepted(upd)
      }
    }
    case _ =>
  }
}

class CreditCardValidator(orderProcessor: ActorRef) extends Actor {
  import Bootstrap.ec
  def receive = {
    case ccvr: CreditCardValidationRequested => {
      //println(s"CreditCardValidationRequested : $ccvr")
      Broadcaster.pushValidation(ccvr)
      val sdr = sender
      val msg = ccvr
      Future {
        Thread.sleep(1000)
        val ccv = CreditCardValidated(msg.order.id)
        orderProcessor.tell(Message.create(ccv), sdr)
      }
    }
    case _ =>
  }
}

class OrdersHandler extends Actor {

  def receive = {
    case OrderAccepted(upd) => {
      Broadcaster.pushValidated(OrderAccepted(upd))
      //println("received event %s" format upd)
    }
    case _ =>
  }
}