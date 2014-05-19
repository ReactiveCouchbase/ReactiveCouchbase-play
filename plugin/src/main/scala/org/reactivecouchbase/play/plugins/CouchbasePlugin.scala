package org.reactivecouchbase.play.plugins

import play.api._
import com.typesafe.config.ConfigObject
import collection.JavaConversions._
import org.reactivecouchbase.{ReactiveCouchbaseDriver, CouchbaseBucket}
import org.reactivecouchbase.play.PlayCouchbase
import org.reactivecouchbase.experimental.CappedBucket
import org.reactivecouchbase.client.ReactiveCouchbaseException

class CouchbasePlugin(implicit app: Application) extends Plugin {

  val logger = Logger("ReactiveCouchbasePlugin")
  var maybeDriver: Option[ReactiveCouchbaseDriver] = None
  var buckets: Map[String, CouchbaseBucket] = Map[String, CouchbaseBucket]()
  override def onStart {
    internalShutdown()
    maybeDriver = Some(ReactiveCouchbaseDriver.apply(
      PlayCouchbase.couchbaseActorSystem,
      new org.reactivecouchbase.Configuration(Play.configuration(app).underlying),
      PlayCouchbase.loggerFacade,
      app.mode match {
        case Mode.Dev => org.reactivecouchbase.Dev()
        case Mode.Test => org.reactivecouchbase.Test()
        case Mode.Prod => org.reactivecouchbase.Prod()
      }
    ))
    logger.info("Starting ReactiveCouchbase plugin ...")
    play.api.Play.configuration(app).getObjectList("couchbase.buckets").map { configs =>
      configs.foreach(conf => connect(maybeDriver.getOrElse(throw new ReactiveCouchbaseException("Error during connection", "Driver not set ...")), conf))
      configs
    }.getOrElse {
      throw new PlayException("No buckets ...", "No buckets found in application.conf")
    }
    logger.info("Starting ReactiveCouchbase plugin done, have fun !!!")
  }
  private def connect(driver: ReactiveCouchbaseDriver, config: ConfigObject) {
    val hosts: List[String] = config.get("host").unwrapped() match {
      case s: String => List(s)
      case a: java.util.ArrayList[String] => a.toList
    }
    val configuration = new Configuration(config.toConfig)
    val bucket = configuration.getString("bucket").getOrElse("default") //config.get("bucket").unwrapped().asInstanceOf[String]
    val alias = configuration.getString("alias").getOrElse(bucket) //config.get("alias").unwrapped().asInstanceOf[String]
    val port = configuration.getString("port").getOrElse("8091") //config.get("port").unwrapped().asInstanceOf[String]
    val base = configuration.getString("base").getOrElse("pools") //config.get("base").unwrapped().asInstanceOf[String]
    val user = configuration.getString("user").getOrElse("") //config.get("user").unwrapped().asInstanceOf[String]
    val pass = configuration.getString("pass").getOrElse("") //config.get("pass").unwrapped().asInstanceOf[String]
    val timeout = configuration.getLong("timeout").getOrElse(1000L) //config.get("timeout").unwrapped().asInstanceOf[String].toLong
    val couchbase: CouchbaseBucket = driver.bucket(hosts, port, base, bucket, alias, user, pass, timeout)
    logger.info(s"""Connection to bucket "${alias}" ...""")
    buckets = buckets + (alias -> couchbase)
  }
  private def internalShutdown() {
    logger.info("ReactiveCouchbase plugin shutdown, disconnecting all buckets ...")
    maybeDriver.map(driver => driver.shutdown())
    buckets = buckets.empty
    CappedBucket.clearCache()
    logger.info("ReactiveCouchbase plugin shutdown done.")
  }

  override def onStop {
    internalShutdown()
  }
}