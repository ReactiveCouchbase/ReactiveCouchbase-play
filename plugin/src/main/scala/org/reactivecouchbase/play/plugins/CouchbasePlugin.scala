package org.reactivecouchbase.play.plugins

import play.api._
import com.typesafe.config.ConfigObject
import collection.JavaConversions._
import org.reactivecouchbase.{ReactiveCouchbaseDriver, CouchbaseBucket}
import org.reactivecouchbase.play.PlayCouchbase
import org.reactivecouchbase.experimental.CappedBucket

class CouchbasePlugin(implicit app: Application) extends Plugin {

  val logger = Logger("ReactiveCouchbasePlugin")
  val driver = ReactiveCouchbaseDriver.apply(
    PlayCouchbase.couchbaseActorSystem,
    new org.reactivecouchbase.Configuration(Play.configuration(app).underlying),
    PlayCouchbase.loggerFacade,
    app.mode match {
      case Mode.Dev => org.reactivecouchbase.Dev()
      case Mode.Test => org.reactivecouchbase.Test()
      case Mode.Prod => org.reactivecouchbase.Prod()
    }
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
  override def onStop {
    logger.info("ReactiveCouchbase plugin shutdown, disconnecting all buckets ...")
    //buckets.foreach { tuple => tuple._2.disconnect() }
    driver.shutdown()
    buckets = buckets.empty
    CappedBucket.clearCache()
    logger.info("ReactiveCouchbase plugin shutdown done.")
  }
}