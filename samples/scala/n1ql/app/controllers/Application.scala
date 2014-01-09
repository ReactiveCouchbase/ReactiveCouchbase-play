package controllers

import play.api.mvc._
import org.reactivecouchbase.play.plugins.CouchbaseN1QLPlugin._
import play.api.libs.json.{Writes, Json}
import play.api.libs.concurrent.Execution.Implicits._
import models._
import models.Persons._
import play.api.Play.current
import org.reactivecouchbase.play.PlayCouchbase

object Application extends Controller {

  implicit val bucket = PlayCouchbase.bucket("default")

  def index = Action.async {
    N1QL( s"""SELECT * from default where datatype = 'person' """ ).toList[Person].map { persons =>
      Ok(Json.toJson(persons)(Writes.list(Persons.fmt)))
    }
  }
}