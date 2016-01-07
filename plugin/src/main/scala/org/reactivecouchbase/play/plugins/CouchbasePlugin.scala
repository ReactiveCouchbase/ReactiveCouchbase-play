package org.reactivecouchbase.play.plugins

import akka.actor.ActorSystem
import play.api._
import com.typesafe.config.{ConfigFactory, ConfigObject}
import collection.JavaConversions._
import org.reactivecouchbase.{ReactiveCouchbaseDriver, CouchbaseBucket}
import org.reactivecouchbase.play.PlayCouchbase
import org.reactivecouchbase.experimental.CappedBucket
import org.reactivecouchbase.client.ReactiveCouchbaseException

class CouchbasePlugin(implicit app: Application) extends Plugin {


  val logger = Logger("ReactiveCouchbasePlugin")
  private var _helper: Option[CouchbaseHelper] = None

  def helper = _helper.getOrElse(throw new RuntimeException(
    "ReactiveCouchbasePlugin error: no CouchbaseHelper available?"))

  override def onStart {
    internalShutdown()
    logger.info("Starting ReactiveCouchbase plugin ...")
    _helper = Some(CouchbaseHelper(app))
    play.api.Play.configuration(app).getObjectList("couchbase.buckets").map { configs =>
      configs.foreach(conf =>
        _helper.foreach { h =>
          h.buckets = h.buckets + connect(h.driver, conf)
        })
      configs
    }.getOrElse {
      throw new PlayException("No buckets ...", "No buckets found in application.conf")
    }
    logger.info("Starting ReactiveCouchbase plugin done, have fun !!!")
  }
  private def connect(driver: ReactiveCouchbaseDriver, config: ConfigObject): (String, CouchbaseBucket) = {
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
    (alias -> couchbase)
  }
  private def internalShutdown() {
    logger.info("ReactiveCouchbase plugin shutdown, disconnecting all buckets ...")
    _helper.foreach { h =>
      h.driver.shutdown()
      h.buckets = h.buckets.empty}

    _helper = None
    CappedBucket.clearCache()
    logger.info("ReactiveCouchbase plugin shutdown done.")
  }

  override def onStop {
    internalShutdown()
  }
}

object CouchbasePlugin {

  /** Returns the current instance of the driver. */
  def driver(implicit app: Application) = current.helper.driver

  /** Returns the current instance of the plugin. */
  def current(implicit app: Application): CouchbasePlugin = app.plugin[CouchbasePlugin] match {
    case Some(plugin) => plugin
    case _            => throw new ReactiveCouchbaseException("Plugin Exception", "plugins.couchbase.CouchbasePlugin' (400 is an arbitrary priority and may be changed to match your needs).")
  }

  /** Returns the current instance of the plugin (from a [[play.Application]] - Scala's [[play.api.Application]] equivalent for Java). */
  def current(app: play.Application): CouchbasePlugin = app.plugin(classOf[CouchbasePlugin]) match {
    case plugin if plugin != null => plugin
    case _                        => throw new ReactiveCouchbaseException("Plugin Exception", "The CouchbasePlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:plugins.couchbase.CouchbasePlugin' (400 is an arbitrary priority and may be changed to match your needs).")
  }
}

private[reactivecouchbase] case class CouchbaseHelper(app: Application) {
  lazy val driver: ReactiveCouchbaseDriver = ReactiveCouchbaseDriver.apply(
    actorSystem,
    new org.reactivecouchbase.Configuration(Play.configuration(app).underlying),
    PlayCouchbase.loggerFacade,
    app.mode match {
      case Mode.Dev  => org.reactivecouchbase.Dev()
      case Mode.Test => org.reactivecouchbase.Test()
      case Mode.Prod => org.reactivecouchbase.Prod()
    })
  lazy val actorSystem = ActorSystem("reactivecouchbase-plugin-system", app.configuration.getConfig("couchbase.akka").map(_.underlying).getOrElse(ConfigFactory.empty()))

  var buckets: Map[String, CouchbaseBucket] = Map[String, CouchbaseBucket]()
}