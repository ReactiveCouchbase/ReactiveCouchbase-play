package controllers

import play.api.mvc._
import org.reactivecouchbase.play.PlayCouchbase
import play.api.Play
import java.util.UUID
import scala.concurrent.Future
import play.api.libs.json.{JsObject, Json}
import org.reactivecouchbase.CouchbaseRWImplicits.jsObjectToDocumentWriter
import java.util.concurrent.atomic.AtomicLong
import org.reactivecouchbase.Couchbase

object Application extends Controller {

  def bucket = PlayCouchbase.bucket("persons")(Play.current)
  implicit val ec = PlayCouchbase.couchbaseExecutor(Play.current)

  def index = Action {
    Ok(views.html.index())
  }

  def createNoPluginJson = Action.async(parse.json) { request =>
    Future {
      val start = System.currentTimeMillis()
      bucket.couchbaseClient.set(UUID.randomUUID().toString, Json.stringify(request.body)).get()
      counter.incrementAndGet()
      adder.addAndGet(System.currentTimeMillis() - start)
      if ( adder.get() % 500 == 0 ) {
        println(s"average ${adder.get() / counter.get()} ms.")
      }
      Ok("")
    }
  }

  def createNoPluginText = Action.async(parse.text) { request =>
    Future {
      val start = System.currentTimeMillis()
      bucket.couchbaseClient.set(UUID.randomUUID().toString, request.body).get()
      counter.incrementAndGet()
      adder.addAndGet(System.currentTimeMillis() - start)
      if ( adder.get() % 500 == 0 ) {
        println(s"average ${adder.get() / counter.get()} ms.")
      }
      Ok("")
    }
  }

  val counter = new AtomicLong(0L)
  val adder = new AtomicLong(0L)

  def createPluginJson = Action.async(parse.json) { request =>
    val start = System.currentTimeMillis()
    Couchbase.set(UUID.randomUUID().toString, request.body.as[JsObject])(bucket, jsObjectToDocumentWriter, ec).map {
      _ =>
        counter.incrementAndGet()
        adder.addAndGet(System.currentTimeMillis() - start)
        if ( adder.get() % 500 == 0 ) {
          println(s"average ${adder.get() / counter.get()} ms.")
        }
        Ok("")
    }
  }
}