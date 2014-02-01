package models

import play.api.libs.json._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

case class Person(name: String, surname: String, age: Int)

object Persons {
  implicit val fmt = Json.format[Person]

  def read(key: String): Future[JsResult[JsValue]] = ???
  def update(doc: JsValue): Future[JsResult[JsValue]] = ???

  def readAndUpdate(id: String, block : (JsValue) => JsValue): Future[JsResult[JsValue]] = {
    read(id).flatMap {
      case JsSuccess(document, _) => update(block(document))
      case error: JsError => Future.successful(error)
    }
  }

  def readAndUpdate2(id: String, block : (JsValue) => JsValue ) : Future[JsResult[JsValue]] = {
    read(id).map( _ match {
      case JsSuccess(document, _) => {
        val documentToUpdate = block(document)
        Await.result(update(documentToUpdate), Duration(30, TimeUnit.MICROSECONDS))
      }
      case error@JsError(_) => error
    }
    )

    Future.f("o")
  }
}
