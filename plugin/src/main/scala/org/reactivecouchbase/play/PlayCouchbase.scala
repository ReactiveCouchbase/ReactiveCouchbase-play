package org.reactivecouchbase.play

import com.couchbase.client.CouchbaseClient
import org.reactivecouchbase.experimental.CappedBucket
import org.reactivecouchbase.play.plugins.CouchbasePlugin
import org.reactivecouchbase.{CouchbaseBucket, LoggerLike, RCLogger}
import play.api._

import scala.concurrent.ExecutionContext

object PlayCouchbase {

  private[reactivecouchbase] val usePlayEC = true
  private[reactivecouchbase] val timeout = 1000
  private[reactivecouchbase] val initMessage = "The CouchbasePlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:org.reactivecouchbase.play.plugins.CouchbasePlugin' (400 is an arbitrary priority and may be changed to match your needs)."
  private[reactivecouchbase] val connectMessage = "The CouchbasePlugin doesn't seems to be connected to a Couchbase server. Maybe an error occured!"
  private[reactivecouchbase] val loggerFacade = PlayLogger.logger("ReactiveCouchbase")

  def defaultBucket(implicit app: Application): CouchbaseBucket = app.plugin[CouchbasePlugin] match {
    case Some(plugin) => plugin.buckets.headOption.getOrElse(throw new PlayException("CouchbasePlugin Error", connectMessage))._2
    case _ => throw new PlayException("CouchbasePlugin Error", initMessage)
  }

  def bucket(bucket: String)(implicit app: Application): CouchbaseBucket = buckets(app).get(bucket).getOrElse(throw new PlayException(s"Error with bucket $bucket", s"Bucket '$bucket' is not defined"))
  def cappedBucket(b: String, max: Int, reaper: Boolean = true)(implicit app: Application): CappedBucket = {
    CappedBucket.apply(() => PlayCouchbase.bucket(b), PlayCouchbase.bucket(b).driver.executor(), max, reaper)
  }
  def client(bucket: String)(implicit app: Application): CouchbaseClient = buckets(app).get(bucket).flatMap(_.client).getOrElse(throw new PlayException(s"Error with bucket $bucket", s"Bucket '$bucket' is not defined or client is not connected"))

  def buckets(implicit app: Application): Map[String, CouchbaseBucket] = app.plugin[CouchbasePlugin] match {
    case Some(plugin) => plugin.buckets
    case _ => throw new PlayException("CouchbasePlugin Error", initMessage)
  }

  def couchbaseExecutor(implicit app: Application): ExecutionContext = {
    app.configuration.getObject("couchbase.execution-context.execution-context") match {
      case Some(_) => app.plugin[CouchbasePlugin].get.actorSystem.dispatcher //couchbaseActorSystem.dispatchers.lookup("couchbase.execution-context.execution-context")
      case _ => {
        if (usePlayEC)
          play.api.libs.concurrent.Execution.Implicits.defaultContext
        else
          throw new PlayException("Configuration issue", "You have to define a 'couchbase.akka' object in the application.conf file.")
      }
    }
  }
}

object PlayLogger extends LoggerLike {

  val logger: org.slf4j.Logger = Logger.underlyingLogger

  def logger(name: String): LoggerLike = new RCLogger(Logger(name).underlyingLogger)

  def logger[T](clazz: Class[T]): LoggerLike = new RCLogger(Logger(clazz).underlyingLogger)
}

