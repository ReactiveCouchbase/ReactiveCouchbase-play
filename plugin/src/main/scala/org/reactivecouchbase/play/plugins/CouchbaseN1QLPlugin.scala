package org.reactivecouchbase.play.plugins

import play.api.{PlayException, Plugin, Application, Play}
import play.api.libs.ws.WS
import play.api.libs.ws.WS.WSRequestHolder
import scala.Some
import org.reactivecouchbase.N1QLQuery

// TODO : plug with N1QL support from core

class CouchbaseN1QLPlugin(app: Application) extends Plugin {

  var queryBase: Option[WSRequestHolder] = None

  override def onStart {
    val host = Play.configuration.getString("couchbase.n1ql.host").getOrElse(throw new PlayException("Cannot find N1QL host", "Cannot find N1QL host in couchbase.n1ql conf."))
    val port = Play.configuration.getString("couchbase.n1ql.port").getOrElse(throw new PlayException("Cannot find N1QL port", "Cannot find N1QL port in couchbase.n1ql conf."))
    queryBase = Some(WS.url(s"http://${host}:${port}/query"))
  }
}

object CouchbaseN1QLPlugin {

  lazy val N1QLPlugin = Play.current.plugin[CouchbaseN1QLPlugin] match {
    case Some(plugin) => plugin
    case _ => throw new PlayException("CouchbaseN1QLPlugin Error", "Cannot find an instance of CouchbaseN1QLPlugin.")
  }

  def N1QL(query: String): N1QLQuery = {
    new N1QLQuery(query, N1QLPlugin.queryBase.getOrElse(throw new PlayException("Cannot find N1QL connection", "Cannot find N1QL connection.")))
  }
}
