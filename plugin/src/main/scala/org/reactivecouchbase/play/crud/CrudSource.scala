package org.reactivecouchbase.play.crud

import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.json._
import com.couchbase.client.protocol.views.{ComplexKey, Stale, Query, View}
import play.api.libs.iteratee.{Enumeratee, Iteratee, Enumerator}
import play.api.mvc._
import org.reactivecouchbase.{CouchbaseRWImplicits, Couchbase, CouchbaseBucket}
import java.util.UUID
import play.core.Router
import play.api.libs.json.JsObject
import org.reactivecouchbase.client.TypedRow

// Higly inspired (not to say copied ;)) from https://github.com/mandubian/play-autosource
class CouchbaseCrudSource[T:Format](bucket: CouchbaseBucket, idKey: String = "_id") {

  import org.reactivecouchbase.CouchbaseRWImplicits._

  val reader: Reads[T] = implicitly[Reads[T]]
  val writer: Writes[T] = implicitly[Writes[T]]
  implicit val ctx: ExecutionContext = bucket.driver.executor()

  def insert(t: T): Future[String] = {
    val id: String = UUID.randomUUID().toString
    val json = writer.writes(t).as[JsObject]
    json \ idKey match {
      case _: JsUndefined => {
        val newJson = json ++ Json.obj(idKey -> JsString(id))
        Couchbase.set(id, newJson)(bucket, CouchbaseRWImplicits.jsObjectToDocumentWriter, ctx).map(_ => id)(ctx)
      }
      case actualId: JsString => {
        Couchbase.set(actualId.value, json)(bucket, CouchbaseRWImplicits.jsObjectToDocumentWriter, ctx).map(_ => actualId.value)(ctx)
      }
      case _ => throw new RuntimeException(s"Field with $idKey already exists and not of type JsString")
    }
  }

  def get(id: String): Future[Option[(T, String)]] = {
    Couchbase.get[T]( id )(bucket ,reader, ctx).map( _.map( v => ( v, id ) ) )(ctx)
  }

  def delete(id: String): Future[Unit] = {
    Couchbase.delete(id)(bucket, ctx).map(_ => ())
  }

  def update(id: String, t: T): Future[Unit] = {
    Couchbase.replace(id, t)(bucket, writer, ctx).map(_ => ())
  }

  def updatePartial(id: String, upd: JsObject): Future[Unit] = {
    get(id).flatMap { opt =>
      opt.map { t =>
        val json = Json.toJson(t._1)(writer).as[JsObject]
        val newJson = json.deepMerge(upd)
        Couchbase.replace((json \ idKey).as[JsString].value, newJson)(bucket, CouchbaseRWImplicits.jsObjectToDocumentWriter, ctx).map(_ => ())
      }.getOrElse(throw new RuntimeException(s"Cannot find id $id"))
    }
  }

  def batchInsert(elems: Enumerator[T]): Future[Int] = {
    elems(Iteratee.foldM[T, Int](0)( (s, t) => insert(t).map(_ => s + 1))).flatMap(_.run)
  }

  def find(sel: (View, Query), limit: Int = 0, skip: Int = 0)(implicit ctx: ExecutionContext): Future[Seq[(T, String)]] = {
    var query = sel._2
    if (limit != 0) query = query.setLimit(limit)
    if (skip != 0) query = query.setSkip(skip)
    Couchbase.search[JsObject](sel._1)(query)(bucket, CouchbaseRWImplicits.documentAsJsObjectReader, ctx).toList(ctx).map { l =>
      l.map { i =>
        val t = reader.reads(i.document) match {
          case e:JsError => throw new RuntimeException("Document does not match object")
          case s:JsSuccess[T] => s.get
        }
        i.document \ idKey match {
          case actualId: JsString => (t, actualId.value)
          case _ => (t, i.id.get)
        }
      }
    }
  }

  def findStream(sel: (View, Query), skip: Int = 0, pageSize: Int = 0)(implicit ctx: ExecutionContext): Enumerator[Iterator[(T, String)]] = {
    var query = sel._2
    if (skip != 0) query = query.setSkip(skip)
    val futureEnumerator = Couchbase.search[JsObject](sel._1)(query)(bucket, CouchbaseRWImplicits.documentAsJsObjectReader, ctx).toList(ctx).map { l =>
      val size = if(pageSize != 0) pageSize else l.size
      Enumerator.enumerate(l.map { i =>
        val t = reader.reads(i.document) match {
          case e:JsError => throw new RuntimeException("Document does not match object")
          case s:JsSuccess[T] => s.get
        }
        i.document \ idKey match {
          case actualId: JsString => (t, actualId.value)
          case _ => (t, i.id.get)
        }
      }.grouped(size).map(_.iterator))
    }
    Enumerator.flatten(futureEnumerator)
  }

  def batchDelete(sel: (View, Query)): Future[Unit] = {
    /*Couchbase.search[JsObject](sel._1)(sel._2)(bucket, CouchbaseRWImplicits.documentAsJsObjectReader, ctx).toList(ctx).map { list =>
      list.map { t =>
        delete(t.id.get)
      }
    }*/
    val extract = { tr: TypedRow[JsObject] => tr.id.get }
    Couchbase.search[JsObject](sel._1)(sel._2)(bucket, CouchbaseRWImplicits.documentAsJsObjectReader, ctx).toEnumerator.map { enumerator =>
      Couchbase.deleteStreamWithKey[TypedRow[JsObject]](extract, enumerator)(bucket, ctx)
    }.map(_ => ())
  }

  def batchUpdate(sel: (View, Query), upd: JsObject): Future[Unit] = {
    /*Couchbase.search[T](sel._1)(sel._2)(bucket, reader, ctx).toList(ctx).map { list =>
      list.map { t =>
        val json = Json.toJson(t.document)(writer).as[JsObject]
        val newJson = json.deepMerge(upd)
        Couchbase.replace(t.id.get, newJson)(bucket, CouchbaseRWImplicits.jsObjectToDocumentWriter, ctx).map(_ => ())
      }
    } */
    Couchbase.search[T](sel._1)(sel._2)(bucket, reader, ctx).toEnumerator.map { enumerator =>
      Couchbase.replaceStream(enumerator.through(Enumeratee.map { t =>
        val json = Json.toJson(t.document)(writer).as[JsObject]
        (t.id.get, json.deepMerge(upd))
      }))(bucket, CouchbaseRWImplicits.jsObjectToDocumentWriter, ctx).map(_ => ())
    }
  }

  def view(docName: String, viewName: String): Future[View] = {
    Couchbase.view(docName, viewName)(bucket, ctx)
  }
}

trait CrudController extends Controller {
  def insert: EssentialAction

  def get(id: String): EssentialAction
  def delete(id: String): EssentialAction
  def update(id: String): EssentialAction
  def updatePartial(id: String): EssentialAction

  def find: EssentialAction
  def findStream: EssentialAction

  def batchInsert: EssentialAction
  def batchDelete: EssentialAction
  def batchUpdate: EssentialAction
}

abstract class CrudRouterController(implicit idBindable: PathBindable[String])
  extends Router.Routes
  with CrudController {

  private var path: String = ""

  private val Slash        = "/?".r
  private val Id           = "/([^/]+)/?".r
  private val Partial      = "/([^/]+)/partial".r
  private val Find         = "/find/?".r
  private val Batch        = "/batch/?".r
  private val Stream       = "/stream/?".r

  def withId(id: String, action: String => EssentialAction) =
    idBindable.bind("id", id).fold(badRequest, action)

  def setPrefix(prefix: String) {
    path = prefix
  }

  def prefix = path
  def documentation = Nil
  def routes = new scala.runtime.AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) = {
      if (rh.path.startsWith(path)) {
        (rh.method, rh.path.drop(path.length)) match {
          case ("GET",    Stream())    => findStream
          case ("GET",    Id(id))      => withId(id, get)
          case ("GET",    Slash())     => find

          case ("PUT",    Partial(id)) => withId(id, updatePartial)
          case ("PUT",    Id(id))      => withId(id, update)
          case ("PUT",    Batch())     => batchUpdate

          case ("POST",   Batch())     => batchInsert
          case ("POST",   Find())      => find
          case ("POST",   Slash())     => insert

          case ("DELETE", Batch())     => batchDelete
          case ("DELETE", Id(id))      => withId(id, delete)
          case _                       => default(rh)
        }
      } else {
        default(rh)
      }
    }

    def isDefinedAt(rh: RequestHeader) =
      if (rh.path.startsWith(path)) {
        (rh.method, rh.path.drop(path.length)) match {
          case ("GET",    Stream()   | Id(_)    | Slash()) => true
          case ("PUT",    Partial(_) | Id(_)    | Batch()) => true
          case ("POST",   Batch()    | Slash())            => true
          case ("DELETE", Batch()    | Id(_))              => true
          case _ => false
        }
      } else {
        false
      }
  }
}

abstract class CouchbaseCrudSourceController[T](implicit format: Format[T], app: play.api.Application) extends CrudRouterController {

  def bucket: CouchbaseBucket
  def defaultDesignDocname = ""
  def defaultViewName = ""
  def idKey = "_id"

  lazy val res = new CouchbaseCrudSource[T](bucket, idKey)

  lazy implicit val ctx = bucket.driver.executor()

  val writerWithId = Writes[(T, String)] {
    case (t, id) => {
      val jsObj = res.writer.writes(t).as[JsObject]
      (jsObj \ idKey) match {
        case _: JsUndefined => jsObj ++ Json.obj(idKey -> id)
        case actualId => jsObj
      }
    }
  }

  def docName(name: String) = {
    if (play.api.Play.isDev(app)) s"dev_$name" else name
  }

  def insert: EssentialAction = Action.async(parse.json) { request =>
    Json.fromJson[T](request.body)(res.reader).map{ t =>
      res.insert(t).map{ id => Ok(Json.obj("id" -> id)) }
    }.recoverTotal{ e => Future(BadRequest(JsError.toFlatJson(e))) }
  }

  def get(id: String): EssentialAction = Action.async {
    res.get(id).map{
      case None    => NotFound(s"ID '${id}' not found")
      case Some(tid) => {
        val jsObj = Json.toJson(tid._1)(res.writer).as[JsObject]
        (jsObj \ idKey) match {
          case _: JsUndefined => Ok( jsObj ++ Json.obj(idKey -> JsString(id)) )
          case actualId => Ok( jsObj )
        }
      }
    }
  }

  def delete(id: String): EssentialAction = Action.async {
    res.delete(id).map{ le => Ok(Json.obj("id" -> id)) }
  }

  def update(id: String): EssentialAction = Action.async(parse.json) { request =>
    Json.fromJson[T](request.body)(res.reader).map{ t =>
      res.update(id, t).map{ _ => Ok(Json.obj("id" -> id)) }
    }.recoverTotal{ e => Future(BadRequest(JsError.toFlatJson(e))) }
  }

  def find: EssentialAction = Action.async { request =>
    val (queryObject, query) = QueryObject.extractQuery(request, defaultDesignDocname, defaultViewName)
    res.view(queryObject.docName, queryObject.view).flatMap { view =>
      res.find((view, query))
    }.map( s => Ok(Json.toJson(s)(Writes.seq(writerWithId))))
  }

  def findStream: EssentialAction = Action.async { request =>
    val (queryObject, query) = QueryObject.extractQuery(request, defaultDesignDocname, defaultViewName)
    res.view(queryObject.docName, queryObject.view).map { view =>
      res.findStream((view, query), 0, 0)
    }.map { s => Ok.stream(
      s.map( it => Json.toJson(it.toSeq)(Writes.seq(writerWithId)) ).andThen(Enumerator.eof) )
    }
  }

  def updatePartial(id: String): EssentialAction = Action.async(parse.json) { request =>
    Json.fromJson[JsObject](request.body)(CouchbaseRWImplicits.documentAsJsObjectReader).map{ upd =>
      res.updatePartial(id, upd).map{ _ => Ok(Json.obj("id" -> id)) }
    }.recoverTotal{ e => Future(BadRequest(JsError.toFlatJson(e))) }
  }

  def batchInsert: EssentialAction = Action.async(parse.json) { request =>
    Json.fromJson[Seq[T]](request.body)(Reads.seq(res.reader)).map{ elems =>
      res.batchInsert(Enumerator(elems:_*)).map{ nb => Ok(Json.obj("nb" -> nb)) }
    }.recoverTotal{ e => Future(BadRequest(JsError.toFlatJson(e))) }
  }

  def batchDelete: EssentialAction = Action.async { request =>
    val (queryObject, query) = QueryObject.extractQuery(request, defaultDesignDocname, defaultViewName)
    res.view(queryObject.docName, queryObject.view).flatMap { view =>
      res.batchDelete((view, query)).map{ _ => Ok("deleted") }
    }
  }

  def batchUpdate: EssentialAction = Action.async(parse.json) { request =>
    val (queryObject, query) = QueryObject.extractQuery(request, defaultDesignDocname, defaultViewName)
    Json.fromJson[JsObject](request.body)(CouchbaseRWImplicits.documentAsJsObjectReader).map{ upd =>
      res.view(queryObject.docName, queryObject.view).flatMap { view =>
        res.batchUpdate((view, query), upd).map{ _ => Ok("updated") }
      }
    }.recoverTotal{ e => Future(BadRequest(JsError.toFlatJson(e))) }
  }
}

case class QueryObject(
  docName: String,
  view: String,
  q: Option[String],
  from: Option[String],
  to: Option[String],
  limit: Option[Int],
  descending: Option[Boolean],
  skip: Option[Int]
)

object QueryObject {

  def extractQueryObject[T](request: Request[T], defaultDesignDocname: String, defaultViewName: String): QueryObject = {
    val q = request.queryString.get("q").flatMap(_.headOption)
    val limit = request.queryString.get("limit").flatMap(_.headOption.map(_.toInt))
    val descending = request.queryString.get("descending").flatMap(_.headOption.map(_.toBoolean))
    val skip = request.queryString.get("skip").flatMap(_.headOption.map(_.toInt))
    val from = request.queryString.get("from").flatMap(_.headOption)
    val to = request.queryString.get("to").flatMap(_.headOption)
    val v = request.queryString.get("view").flatMap(_.headOption).getOrElse(defaultViewName)
    val doc = request.queryString.get("doc").flatMap(_.headOption).getOrElse(defaultDesignDocname)
    val maybeQueryObject = QueryObject(doc, v, q, from, to, limit, descending, skip)
    request.body match {
      case AnyContentAsJson(json) => {
        QueryObject(
          (json \ "docName").asOpt[String].getOrElse(defaultDesignDocname),
          (json \ "view").asOpt[String].getOrElse(defaultViewName),
          (json \ "q").asOpt[String],
          (json \ "from").asOpt[String],
          (json \ "to").asOpt[String],
          (json \ "limit").asOpt[Int],
          (json \ "descending").asOpt[Boolean],
          (json \ "skip").asOpt[Int]
        )
      }
      case _ => maybeQueryObject
    }
  }

  def extractQuery[T](request: Request[T], defaultDesignDocname: String, defaultViewName: String): (QueryObject, Query) = {
    val q = extractQueryObject( request, defaultDesignDocname, defaultViewName )
    ( q, extractQuery( q ) )
  }

  def extractQuery(queryObject: QueryObject): Query = {
    var query = new Query().setIncludeDocs(true).setStale(Stale.FALSE)
    if (queryObject.q.isDefined) {
      query = query.setRangeStart(ComplexKey.of(queryObject.q.get))
        .setRangeEnd(ComplexKey.of(s"${queryObject.q.get}\uefff"))
    } else if (queryObject.from.isDefined && queryObject.to.isDefined) {
      query = query.setRangeStart(queryObject.from.get).setRangeEnd(queryObject.to.get)
    }
    if (queryObject.limit.isDefined) {
      query = query.setLimit(queryObject.limit.get)
    }
    if (queryObject.skip.isDefined) {
      query = query.setSkip(queryObject.skip.get)
    }
    if (queryObject.descending.isDefined) {
      query = query.setDescending(queryObject.descending.get)
    }
    query
  }
}

