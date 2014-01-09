package org.reactivecouchbase.play.plugins

import play.api._
import scala.reflect.io.Directory
import scalax.io.Resource
import play.api.libs.Codecs
import play.api.libs.json._
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import net.spy.memcached.ops.OperationStatus
import org.reactivecouchbase.{Couchbase, CouchbaseBucket}
import org.reactivecouchbase.play.PlayCouchbase

case class DocumentDescription(fileName:String, bucket:CouchbaseBucket , bytes:Array[Byte]) {

  lazy val hash = Codecs.sha1(bytes)
  lazy val json = Json.parse(bytes)

  lazy val name = (json \ "name").asOpt[String].getOrElse(fileName)

  def put(dev:Boolean)(implicit ec:ExecutionContext):Future[OperationStatus] = {
    Couchbase.createDesignDoc(DocumentDescription.name(name, dev), json.as[JsObject])(bucket, ec)
  }

}

object DocumentDescription {
  def name(name:String, dev:Boolean):String = {
    if(dev) "dev_"+name else name
  }
}

object CouchbaseEvolutions {

  lazy val logger = Logger(classOf[CouchbaseEvolutionsPlugin])

  /*
  TODO : There is no way to list design documents of the bucket using the client
  See : http://www.couchbase.com/issues/browse/JCBC-306
   */
  def designDocuments(bucket: CouchbaseBucket)(implicit ec:ExecutionContext):List[String] = {
    List()
  }

  def perform(docs: Iterator[DocumentDescription], synchronise:Boolean, dev:Boolean = true)(implicit bucket:CouchbaseBucket, ec:ExecutionContext) = {

    if(synchronise)
      designDocuments(bucket).filterNot { name =>
        docs.exists(nd => DocumentDescription.name(nd.name, dev) == DocumentDescription.name(name, dev))
      }.foldLeft(List[OperationStatus]()) {
        (res, doc) => {
          val name = DocumentDescription.name(doc, dev)
          val status = Await.result(Couchbase.deleteDesignDoc(name), Duration(1, TimeUnit.SECONDS))
          if(!status.isSuccess)
            logger.warn(s"Fail to delete design document $name : ${status.getMessage}")
          res :+ status
        }
      }

    docs.foldLeft(List[OperationStatus]()) {
      (res, doc) => {
        val status = Await.result(doc.put(dev), Duration(1, TimeUnit.SECONDS))
        if(!status.isSuccess)
          logger.warn(s"Fail to create design document ${doc.name} : ${status.getMessage}")
        res :+ status
      }
    }

  }

}

class CouchbaseEvolutionsPlugin(app: Application) extends Plugin {

  import CouchbaseEvolutions._

  lazy val conf = app.configuration.getConfig("couchbase").getOrElse(Configuration.empty)
  lazy val evolutionConf = conf.getConfig("evolutions").getOrElse(Configuration.empty)

  lazy val docs = evolutionConf.getString("documents").getOrElse("conf/views")

  implicit val ec = PlayCouchbase.couchbaseExecutor(app)

  override lazy val enabled = !evolutionConf.getBoolean("disabled").exists(_ == true)

  /**
   * Checks the evolutions state.
   */
  override def onStart() {

    PlayCouchbase.buckets(app).foreach {
      case(name, bucket) => {
        implicit val b = bucket

        val bucketConf = evolutionConf.getConfig(name).getOrElse(Configuration.empty)
        val applyEvolutions = bucketConf.getBoolean("apply").exists(_ == true)
        val synchronise = bucketConf.getBoolean("synchronise").exists(_ == true)

        withLock() {
          Play.getExistingFile(s"$docs/$name")(app).map { folder =>
            logger.debug(s"Search couchbase documents for bucket $name in ${folder.getAbsolutePath}")
            val documents = new Directory(folder).files.filter(_.toFile.name.endsWith(".json")).map { path =>
              DocumentDescription(path.name.replaceAll("""\.[^.]*$""", ""), bucket, Resource.fromInputStream(path.toFile.inputStream()).byteArray)
            }

            if(!documents.isEmpty) {
              app.mode match {
                case Mode.Test => perform(documents, synchronise)
                case Mode.Dev if applyEvolutions => perform(documents, synchronise)
                case Mode.Prod if applyEvolutions => perform(documents, synchronise, false)
                case Mode.Prod => {
                  Logger("play").warn(s"Your production bucket $name needs couchbase design documents evolution!\n\n")
                  Logger("play").warn(s"Run with -Dcouchbase.evolutions.$name.apply=true if you want to run them automatically (be careful)")

                  throw new PlayException(s"Couchbase evolution should be applied, set couchbase.evolutions.$name.apply=true in application.conf", null)
                }
                case _ => new PlayException(s"Couchbase evolution should be applied, set couchbase.evolutions.$name.apply=true in application.conf", null)
              }
            }

          }
        }
      }
    }
  }

  def withLock()(block: => Unit)(implicit bucket: CouchbaseBucket, ec: ExecutionContext) {
    if (evolutionConf.getBoolean("use.locks").getOrElse(true)) {
      lock()
      try {
        block
      } finally {
        unlock()
      }
    } else {
      block
    }
  }

  def lock(attempts: Int = 5)(implicit bucket: CouchbaseBucket, ec:ExecutionContext) {
    try {
      logger.debug(s"Attempting to acquire lock for couchbase evolutions on bucket ${bucket.alias}...")

      val maybe = Await.result(Couchbase.get[JsValue]("couchbase_evolution_lock"), Duration(1, TimeUnit.SECONDS))


      if(maybe.map(json => (json \ "locked").as[Int]).getOrElse(0) == 1) throw new LockedEvolution(bucket)

      val status = Await.result(Couchbase.set("couchbase_evolution_lock", Json.obj("locked" -> JsNumber(1))), Duration(1, TimeUnit.SECONDS))

      if(!status.isSuccess) throw new LockedEvolution(bucket)
      logger.debug("locked!")

    } catch {
      case e:LockedEvolution =>
        if (attempts == 0) throw e
        else {
          logger.warn(e.content() + ", sleeping for 1 sec")
          Thread.sleep(1000)
          lock(attempts - 1)
        }
    }
  }

  def unlock()(implicit bucket: CouchbaseBucket, ec:ExecutionContext) {
    logger.debug(s"Attempting to release lock for couchbase evolutions on bucket ${bucket.alias}...")
    Await.result(Couchbase.delete("couchbase_evolution_lock"), Duration(1, TimeUnit.SECONDS))
    logger.debug("unlocked!")
  }
}

case class LockedEvolution(bucket:CouchbaseBucket) extends PlayException.RichDescription(
  s"Bucket ${bucket.alias} locked",
  "Exception while attempting to lock couchbase evolutions (other node probably has lock)"
  ) {

  def subTitle(): String = s"Bucket ${bucket.alias} locked"

  def content(): String = "Exception while attempting to lock couchbase evolutions (other node probably has lock)"

  def htmlDescription(): String = {

    <span>Unable to run couchbase evolution on bucket {bucket.alias}</span>

  }.mkString
}
