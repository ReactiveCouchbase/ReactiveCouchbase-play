package controllers

import org.reactivecouchbase.play.crud.CouchbaseCrudSourceController
import models.Persons.fmt
import models.Person
import org.reactivecouchbase.play.PlayCouchbase
import play.api.Play.current

object PersonsController extends CouchbaseCrudSourceController[Person] {
  def bucket = PlayCouchbase.bucket("persons")
  override def defaultViewName = "by_person"
  override def defaultDesignDocname = "persons"
}