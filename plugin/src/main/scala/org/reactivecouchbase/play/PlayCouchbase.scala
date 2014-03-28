package org.reactivecouchbase.play

import com.couchbase.client.CouchbaseClient
import play.api._
import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import org.reactivecouchbase.{RCLogger, LoggerLike, CouchbaseBucket}
import org.reactivecouchbase.play.plugins.CouchbasePlugin
import org.reactivecouchbase.experimental.CappedBucket

object PlayCouchbase {

  private[reactivecouchbase] val usePlayEC = true
  private[reactivecouchbase] val timeout = 1000
  private[reactivecouchbase] val initMessage = "The CouchbasePlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:package org.reactivecouhbase.play.CouchbasePlugin' (400 is an arbitrary priority and may be changed to match your needs)."
  private[reactivecouchbase] val connectMessage = "The CouchbasePlugin doesn't seems to be connected to a Couchbase server. Maybe an error occured!"

  private[reactivecouchbase] val couchbaseActorSystem = ActorSystem("reactivecouchbase-plugin-system")
  private[reactivecouchbase] val loggerFacade = PlayLogger.logger("ReactiveCouchbase")

  def defaultBucket(implicit app: Application): CouchbaseBucket = app.plugin[CouchbasePlugin] match {
    case Some(plugin) => plugin.buckets.headOption.getOrElse(throw new PlayException("CouchbasePlugin Error", connectMessage))._2
    case _ => throw new PlayException("CouchbasePlugin Error", initMessage)
  }

  def bucket(bucket: String)(implicit app: Application): CouchbaseBucket = buckets(app).get(bucket).getOrElse(throw new PlayException(s"Error with bucket $bucket", s"Bucket '$bucket' is not defined"))
  def cappedBucket(bucket: String, max: Int, reaper: Boolean = true)(implicit app: Application): CappedBucket = {
    buckets(app).get(bucket).map { bucket =>
      CappedBucket(bucket, bucket.driver.executor(), max, reaper)
    }.getOrElse(throw new PlayException(s"Error with bucket $bucket", s"Bucket '$bucket' is not defined"))
  }
  def client(bucket: String)(implicit app: Application): CouchbaseClient = buckets(app).get(bucket).flatMap(_.client).getOrElse(throw new PlayException(s"Error with bucket $bucket", s"Bucket '$bucket' is not defined or client is not connected"))

  def buckets(implicit app: Application): Map[String, CouchbaseBucket] = app.plugin[CouchbasePlugin] match {
    case Some(plugin) => plugin.buckets
    case _ => throw new PlayException("CouchbasePlugin Error", initMessage)
  }

  def couchbaseExecutor(implicit app: Application): ExecutionContext = {
    app.configuration.getObject("couchbase.execution-context.execution-context") match {
      case Some(_) => couchbaseActorSystem.dispatchers.lookup("couchbase.execution-context.execution-context")
      case _ => {
        if (usePlayEC)
          play.api.libs.concurrent.Execution.Implicits.defaultContext
        else
          throw new PlayException("Configuration issue", "You have to define a 'couchbase.execution-context.execution-context' object in the application.conf file.")
      }
    }
  }
}

object PlayLogger extends LoggerLike {

  val logger: org.slf4j.Logger = Logger.underlyingLogger

  def logger(name: String): LoggerLike = new RCLogger(Logger(name).underlyingLogger)

  def logger[T](clazz: Class[T]): LoggerLike = new RCLogger(Logger(clazz).underlyingLogger)
}

