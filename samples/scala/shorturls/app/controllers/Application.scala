package controllers

import play.api.mvc._
import models.ShortURLs
import models.ShortURLs._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index())
  }

  def goTo(id: String) = Action.async {
    ShortURLs.findById(id).map { maybe =>
      maybe.map( url => Redirect(url.originalUrl) ).getOrElse(NotFound)
    }
  }
}