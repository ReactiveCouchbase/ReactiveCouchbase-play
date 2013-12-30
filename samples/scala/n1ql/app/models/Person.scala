package models

import play.api.libs.json._

case class Person(name: String, surname: String, datatype: String = "person")

object Persons {
  implicit val fmt = Json.format[Person]
}
