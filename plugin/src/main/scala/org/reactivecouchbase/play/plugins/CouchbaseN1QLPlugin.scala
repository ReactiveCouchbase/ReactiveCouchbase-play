package org.reactivecouchbase.play.plugins

import play.api.{PlayException, Plugin, Application, Play}
import scala.Some
import org.reactivecouchbase._

class CouchbaseN1QLPlugin(app: Application) extends Plugin {

  var host: String = _
  var port: String = _

  override def onStart {
    host = Play.configuration(app).getString("couchbase.n1ql.host").getOrElse(throw new PlayException("Cannot find N1QL host", "Cannot find N1QL host in couchbase.n1ql conf."))
    port = Play.configuration(app).getString("couchbase.n1ql.port").getOrElse(throw new PlayException("Cannot find N1QL port", "Cannot find N1QL port in couchbase.n1ql conf."))
  }
}

object CouchbaseN1QLPlugin {

  def N1QL(query: String)(implicit app: Application, bucket: CouchbaseBucket): N1QLQuery = {
    lazy val N1QLPlugin = app.plugin[CouchbaseN1QLPlugin] match {
      case Some(plugin) => plugin
      case _ => throw new PlayException("CouchbaseN1QLPlugin Error", "Cannot find an instance of CouchbaseN1QLPlugin.")
    }

    lazy val PlayCouchbasePlugin = app.plugin[CouchbasePlugin] match {
      case Some(plugin) => plugin
      case _ => throw new PlayException("CouchbasePlugin Error", "Cannot find an instance of CouchbasePlugin.")
    }
    CouchbaseN1QL.N1QL(bucket, query, N1QLPlugin.host, N1QLPlugin.port)
  }
}
