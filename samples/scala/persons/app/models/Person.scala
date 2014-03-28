package models

import play.api.libs.json._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

case class Person(name: String, surname: String, age: Int)

object Persons {
  implicit val fmt = Json.format[Person]
}
