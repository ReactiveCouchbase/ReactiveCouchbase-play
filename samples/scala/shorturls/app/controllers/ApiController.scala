package controllers

import play.api._
import play.api.mvc._
import models.{IdGenerator, ShortURLs, ShortURL}
import play.api.libs.json.Json
import models.ShortURLs._
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.{EventSource, Comet}


object ApiController extends Controller {

  def getUrl(id: String) = Action.async {
    ShortURLs.findById(id).map { maybe =>
      maybe.map( url => Ok( Json.toJson(url)) ).getOrElse(NotFound)
    }
  }

  def getAllUrls() = Action.async {
    ShortURLs.findAll().map(urls => Ok( Json.toJson( urls ) ) )
  }

  implicit val toJson = Comet.CometMessage[ShortURL]( url => Json.stringify( ShortURLs.urlWriter.writes( url ) ) )

  def getAllUrlsAsSSE() = Action.async {
    ShortURLs.findAllAsEnumerator().map { enumerator => Ok.chunked( enumerator.through( EventSource() ) ) }
  }

  def pollAll() = Action {
    Ok.chunked( ShortURLs.pollAll().through( EventSource() ) )
  }

  def createUrl() = Action.async { implicit request =>
    ShortURLs.urlForm.bindFromRequest.fold(
      errors => Future(BadRequest(
        Json.obj(
          "status" -> "error",
          "error" -> true,
          "created" -> false,
          "message" -> "You need to pass a non empty url value")
        )
      ),
      url => {
        ShortURLs.findByURL(url).flatMap { maybeUrl =>
          maybeUrl.map { shortUrl =>
            Future(
              Ok(
                Json.obj(
                  "status" -> "existing",
                  "error" -> false,
                  "created" -> true,
                  "message" -> "already exists",
                  "url" -> shortUrl
                )
              )
            )
          }.getOrElse {
            IdGenerator.nextId().flatMap { id =>
              val shortUrl = ShortURL(s"$id", url)
              ShortURLs.save(shortUrl).map { status =>
                status.isSuccess match {
                  case true => Ok(
                    Json.obj(
                      "status" -> "created",
                      "error" -> false,
                      "created" -> true,
                      "message" -> status.getMessage,
                      "url" -> shortUrl
                    )
                  )
                  case false => BadRequest(
                    Json.obj(
                      "status" -> "error",
                      "error" -> true,
                      "created" -> false,
                      "message" -> status.getMessage
                    )
                  )
                }
              }
            }
          }
        }
      }
    )
  }

  def delete(id: String) = Action.async {
    ShortURLs.remove(id).map { status =>
      status.isSuccess match {
        case true => Ok(
          Json.obj(
            "status" -> "deleted",
            "error" -> false,
            "deleted" -> true,
            "message" -> status.getMessage
          )
        )
        case false => BadRequest(
          Json.obj(
            "status" -> "error",
            "error" -> true,
            "deleted" -> false,
            "message" -> status.getMessage
          )
        )
      }
    }
  }
}
