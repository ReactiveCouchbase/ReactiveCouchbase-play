package models

import play.api.libs.json._

case class Person(name: String, surname: String, age: Int)

object Persons {
  implicit val fmt = Json.format[Person]
}
