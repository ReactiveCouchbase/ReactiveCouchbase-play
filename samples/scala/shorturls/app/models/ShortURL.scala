package models

import akka.actor.{Props, Actor, ActorSystem}
import scala.concurrent.{ExecutionContext, Future}
import akka.pattern.ask
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import play.api.libs.json.{Reads, Json}
import com.couchbase.client.protocol.views.{ComplexKey, Stale, Query}
import net.spy.memcached.ops.OperationStatus
import play.api.data.Form
import play.api.data.Forms._
import play.api.Play.current
import play.api.libs.iteratee.{Enumeratee, Enumerator}
import org.reactivecouchbase.play.PlayCouchbase
import org.reactivecouchbase.client.OpResult

case class Counter(value: Long)
case class IncrementAndGet()
class IdGenerator extends Actor {

  import models.ShortURLs._

  def receive = {
    case _:IncrementAndGet ⇒ {
      val customSender = sender
      ShortURLs.bucket.get[Counter](IdGenerator.counterKey)(ShortURLs.counterReader, ShortURLs.ec).map { maybe =>
        maybe.map { value =>
          val newValue = value.copy(value.value + 1L)
          ShortURLs.bucket.set[Counter](IdGenerator.counterKey, newValue)(ShortURLs.counterWriter, ShortURLs.ec).map { status =>
            customSender.tell(newValue.value, self)
          }
        }.getOrElse {
          ShortURLs.bucket.set[Counter](IdGenerator.counterKey, Counter(1L))(ShortURLs.counterWriter, ShortURLs.ec).map { status =>
            customSender.tell(1L, self)
          }
        }
      }
    }
  }
}

object IdGenerator {
  implicit val system = ActorSystem("AgentSystem")
  implicit val timeout = Timeout(2, TimeUnit.SECONDS)
  val generator = system.actorOf(Props[IdGenerator], name = "generator")
  val counterKey = "urlidgenerator"
  def nextId(): Future[Long] = {
    (generator ? IncrementAndGet()).mapTo[Long]
  }
}

case class ShortURL(id: String, originalUrl: String, t: String = "shorturl")

object ShortURLs {
  implicit val fmt = Json.format[ShortURL]
  implicit val urlReader = Json.reads[ShortURL]
  implicit val urlWriter = Json.writes[ShortURL]
  implicit val counterReader = Json.reads[Counter]
  implicit val counterWriter = Json.writes[Counter]
  implicit val ec = PlayCouchbase.couchbaseExecutor

  def bucket = PlayCouchbase.bucket("default")

  val urlForm = Form( "url" -> nonEmptyText )

  def findById(id: String): Future[Option[ShortURL]] = {
    bucket.get[ShortURL](id)
  }

  def findAll(): Future[List[ShortURL]] = {
    bucket.find[ShortURL]("shorturls", "by_url")( new Query().setIncludeDocs(true).setStale(Stale.FALSE) )
  }

  def findAllAsEnumerator(): Future[Enumerator[ShortURL]] = {
    bucket.searchValues[ShortURL]("shorturls", "by_url")( new Query().setIncludeDocs(true).setStale(Stale.FALSE) ).toEnumerator
  }

  def pollAll(): Enumerator[ShortURL] = {
    var i = 0L
    bucket.pollQuery[ShortURL]("shorturls", "by_url", new Query().setIncludeDocs(true).setStale(Stale.FALSE), 1000, { chunk: ShortURL =>
      val old = i
      val actual = chunk.id.toLong
      if (actual > old) {
        i = actual
        true
      } else false
    })
    //repeatQuery[ShortURL]("shorturls", "by_url", new Query().setIncludeDocs(true).setStale(Stale.FALSE))
  }

  def findByURL(url: String): Future[Option[ShortURL]] = {
    val query = new Query()
      .setLimit(1)
      .setIncludeDocs(true)
      .setStale(Stale.FALSE)
      .setRangeStart(ComplexKey.of(url))
      .setRangeEnd(ComplexKey.of(s"$url\uefff"))
    bucket.find[ShortURL]("shorturls", "by_url")(query).map(_.headOption)
  }

  def save(url: ShortURL): Future[OpResult] = {
    bucket.set[ShortURL]( url.id, url )
  }

  def remove(id: String): Future[OpResult] = {
    bucket.delete(id)
  }

  def remove(url: ShortURL): Future[OpResult] = {
    bucket.delete(url.id)
  }
}
