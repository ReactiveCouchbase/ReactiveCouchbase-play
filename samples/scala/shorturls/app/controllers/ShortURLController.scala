package controllers

import org.reactivecouchbase.play.crud.CouchbaseCrudSourceController
import models.ShortURL
import models.ShortURLs.fmt
import org.reactivecouchbase.play.PlayCouchbase
import play.api.Play.current

object ShortURLController extends CouchbaseCrudSourceController[ShortURL] {
  def bucket = PlayCouchbase.bucket("default")
  override def defaultViewName = "by_url"
  override def defaultDesignDocname = "shorturls"
  override def idKey = "id"
}