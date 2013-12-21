package org.reactivecouchbase.play

import play.api._
import com.typesafe.config.ConfigObject
import collection.JavaConversions._
import org.reactivecouchbase.{ReactiveCouchbaseDriver, CouchbaseBucket}

// TODO : plug with couchbase support from core
class CouchbasePlugin(implicit app: Application) extends Plugin {

  val logger = Logger("ReactiveCouchbasePlugin")
  val driver = ReactiveCouchbaseDriver.apply(
    PlayCouchbase.couchbaseActorSystem,
    new org.reactivecouchbase.Configuration(Play.configuration(app).underlying),
    PlayCouchbase.loggerFacade
  )
  var buckets: Map[String, CouchbaseBucket] = Map[String, CouchbaseBucket]()
  override def onStart {
    logger.info("Starting ReactiveCouchbase plugin ...")
    play.api.Play.configuration(app).getObjectList("couchbase.buckets").map { configs =>
      configs.foreach(connect)
      configs
    }.getOrElse {
      throw new PlayException("No buckets ...", "No buckets found in application.conf")
    }
    logger.info("Starting ReactiveCouchbase plugin done, have fun !!!")
  }
  private def connect(config: ConfigObject) {
    val bucket = config.get("bucket").unwrapped().asInstanceOf[String]
    val hosts = config.get("host").unwrapped() match {
      case s: String => List(s)
      case a: java.util.ArrayList[String] => a.toList
    }
    val port = config.get("port").unwrapped().asInstanceOf[String]
    val base = config.get("base").unwrapped().asInstanceOf[String]
    val user = config.get("user").unwrapped().asInstanceOf[String]
    val pass = config.get("pass").unwrapped().asInstanceOf[String]
    val timeout = config.get("timeout").unwrapped().asInstanceOf[String].toLong
    val couchbase: CouchbaseBucket = driver.bucket(hosts.toList, port, base, bucket, user, pass, timeout)
    logger.info(s"""Connection to bucket "${bucket}" ...""")
    buckets = buckets + (bucket -> couchbase.connect())
  }
  override def onStop {
    logger.info("ReactiveCouchbase plugin shutdown, disconnecting all buckets ...")
    buckets.foreach { tuple => tuple._2.disconnect() }
    buckets = buckets.empty
    logger.info("ReactiveCouchbase plugin shutdown done.")
  }
}