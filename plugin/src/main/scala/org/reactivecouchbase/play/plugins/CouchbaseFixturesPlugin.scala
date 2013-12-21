package org.reactivecouchbase.play.plugins

import play.api._
import scala.reflect.io.Directory
import scalax.io.{Codec, Resource}
import play.api.libs.json._
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import org.reactivecouchbase.{CouchbaseRWImplicits, CouchbaseBucket, Couchbase}
import org.reactivecouchbase.play.PlayCouchbase

class CouchbaseFixturesPlugin(app: Application) extends Plugin {

  lazy val conf = app.configuration.getConfig("couchbase").getOrElse(Configuration.empty)
  lazy val evolutionConf = conf.getConfig("fixtures").getOrElse(Configuration.empty)
  lazy val docs = evolutionConf.getString("documents").getOrElse("conf/fixtures")
  implicit val ec = PlayCouchbase.couchbaseExecutor(app)
  override lazy val enabled = !evolutionConf.getBoolean("disabled").exists(_ == true)

  def insertDocuments(id: String, documents: List[Seq[JsObject]], bucket: CouchbaseBucket) = {
    documents.map { seq =>
      seq.map { doc =>
        (doc \ id) match {
          case actualId: JsString => Couchbase.set(actualId.value, doc)(bucket, CouchbaseRWImplicits.jsObjectToDocumentWriter, ec)
          case _ => throw new PlayException("Error while inserting fixture", s"Member named $id not found in object")
        }
      }
    }
  }

  override def onStart() {
    PlayCouchbase.buckets(app).foreach {
      case(name, bucket) => {
        val bucketConf = evolutionConf.getConfig(name).getOrElse(Configuration.empty)
        val applyFixtures = bucketConf.getBoolean("insert").exists(_ == true)
        val id = bucketConf.getString("key").getOrElse("_id")
        Play.getExistingFile(s"$docs/$name")(app).map { folder =>
          val documents = new Directory(folder).files.filter(_.toFile.name.endsWith(".json")).toList.map { path =>
            Json.parse(Resource.fromInputStream(path.toFile.inputStream()).string(Codec.UTF8))
          } filter {
            case array: JsArray => true
            case _ => false
          } map(_.as[JsArray]) map { array =>
            array.value.filter {
              case obj: JsObject => true
              case _ => false
            } map(_.as[JsObject])
          }
          if(!documents.isEmpty) {
            app.mode match {
              case Mode.Test if applyFixtures => insertDocuments(id, documents, bucket)
              case Mode.Dev if applyFixtures => insertDocuments(id, documents, bucket)
              case Mode.Prod if applyFixtures => insertDocuments(id, documents, bucket)
              case Mode.Prod => throw new PlayException(s"Couchbase evolution should be applied, set couchbase.fixtures.$name.insert=true in application.conf", null)
              case _ => new PlayException(s"Couchbase fixtures should be applied, set couchbase.fixtures.$name.insert=true in application.conf", null)
            }
          }
        }
      }
    }
  }
}
