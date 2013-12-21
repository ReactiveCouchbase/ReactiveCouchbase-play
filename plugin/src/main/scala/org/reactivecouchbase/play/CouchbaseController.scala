package org.reactivecouchbase.play

import scala.concurrent.Future
import play.api.mvc._
import play.api.Play.current
import org.reactivecouchbase.{Couchbase, CouchbaseBucket}

trait CouchbaseController { self: Controller =>

  def defaultBucket = PlayCouchbase.defaultBucket(current)
  def buckets = PlayCouchbase.buckets

  def CouchbaseAction(block: CouchbaseBucket => Future[SimpleResult]): EssentialAction = {
    Action.async {
      implicit val client = PlayCouchbase.defaultBucket(current)
      block(client)
    }
  }

  def CouchbaseAction(bucket :String)(block: CouchbaseBucket => Future[SimpleResult]):EssentialAction = {
    Action.async {
      implicit val client = PlayCouchbase.bucket(bucket)(current)
      block(client)
    }
  }

  def CouchbaseReqAction(block: Request[AnyContent] => CouchbaseBucket => Future[SimpleResult]):EssentialAction = {
    Action.async { request =>
      implicit val client = PlayCouchbase.defaultBucket(current)
      block(request)(client)
    }
  }

  def CouchbaseReqAction(bucket :String)(block: Request[AnyContent] => CouchbaseBucket => Future[SimpleResult]):EssentialAction = {
    Action.async { request =>
      implicit val client = PlayCouchbase.bucket(bucket)(current)
      block(request)(client)
    }
  }
}
