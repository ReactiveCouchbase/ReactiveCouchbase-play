package org.reactivecouchbase.play.plugins

import play.api.cache.{CacheAPI, CachePlugin}
import play.api.{PlayException, Application}
import java.util.concurrent.TimeUnit
import net.spy.memcached.transcoders.{Transcoder, SerializingTranscoder}
import java.io.{ObjectOutputStream, ByteArrayOutputStream, ObjectStreamClass}
import org.reactivecouchbase.play.PlayCouchbase

// Highly inspired (not to say copied ;-)) from https://github.com/mumoshu/play2-memcached
class CouchbaseCachePlugin(app: Application) extends CachePlugin {

  lazy val api = new CacheAPI {

    class CustomSerializing extends SerializingTranscoder {

      override protected def deserialize(data: Array[Byte]): java.lang.Object = {
        new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(data)) {
          override protected def resolveClass(desc: ObjectStreamClass) = {
            Class.forName(desc.getName(), false, play.api.Play.current.classloader)
          }
        }.readObject()
      }

      override protected def serialize(obj: java.lang.Object) = {
        val bos: ByteArrayOutputStream = new ByteArrayOutputStream()
        new ObjectOutputStream(bos).writeObject(obj)
        bos.toByteArray()
      }
    }

    lazy val tc = new CustomSerializing().asInstanceOf[Transcoder[Any]]

    def get(key: String) = {
      val future = client.asyncGet(namespace + key, tc)
      try {
        val any = future.get(PlayCouchbase.timeout, TimeUnit.MILLISECONDS)
        Option(
          any match {
            case x: java.lang.Byte => x.byteValue()
            case x: java.lang.Short => x.shortValue()
            case x: java.lang.Integer => x.intValue()
            case x: java.lang.Long => x.longValue()
            case x: java.lang.Float => x.floatValue()
            case x: java.lang.Double => x.doubleValue()
            case x: java.lang.Character => x.charValue()
            case x: java.lang.Boolean => x.booleanValue()
            case x => x
          }
        )
      } catch {
        case e: Throwable =>
          future.cancel(false)
          None
      }
    }

    def set(key: String, value: Any, expiration: Int) {
      client.set(namespace + key, expiration, value, tc)
    }

    def remove(key: String) {
      client.delete(namespace + key)
    }
  }
  lazy val client = PlayCouchbase.client( app.configuration.getString("couchbase.cache.bucket").getOrElse(throw new PlayException("Unspecified bucket", "You have to specify a bucket to use Couchbase as a cache.")))(app)
  lazy val namespace: String = app.configuration.getString("couchbase.cache.namespace").getOrElse("")

  override lazy val enabled = {
    app.configuration.getBoolean("couchbase.cache.enabled").getOrElse(false)
  }
}