package controllers

import play.api.mvc._
import models.{Order, OrderSubmitted}
import org.reactivecouchbase.eventstore.Message
import es.{Broadcaster, Bootstrap}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import es.Bootstrap.ec
import play.api.libs.iteratee.{Enumeratee, Concurrent}
import play.api.libs.json.{Json, JsObject}
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future

object Application extends Controller {

  implicit val timeout = Timeout(5 seconds)
  val toJsString = Enumeratee.map[JsObject] { jso => s"data: ${Json.stringify(jso)}\n\n"}
  val creditCardNumberForm = Form(
    "creditCardNumber" -> text
  )

  def index = Action {
    Ok(views.html.index())
  }

  def sse = Action {
    Ok.feed(Broadcaster.enumerator.through(toJsString)).as("text/event-stream")
  }

  def order = Action.async { implicit request =>
    creditCardNumberForm.bindFromRequest().fold(
      _ => Future(BadRequest("You have to provide a credit card number")),
      creditCardNumber => {
        val message = Message.create(
          OrderSubmitted(
            Order(details = "jelly bean", creditCardNumber = creditCardNumber)
          )
        )
        (Bootstrap.processor ? message).map(_ => Ok("Done !!!"))
      }
    )
  }
}