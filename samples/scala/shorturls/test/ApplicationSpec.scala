package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.test.FakeApplication
import java.util.concurrent.{Executors, CountDownLatch}
import play.api.libs.iteratee
import scala.concurrent.ExecutionContext

import org.junit.runner._

import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.matcher._

import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.Logger


/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner]) 
class ApplicationSpec extends Specification {

  "Application" should {

    "find an some urls" in new WithApplication {
      val home = route(FakeRequest(GET, "/")).get
      val content = contentAsString(home)
      println(content)
      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "application/json")
    }

    "find an some urls 2" in new WithApplication {
      val home = route(FakeRequest(GET, "/")).get
      val content = contentAsString(home)
      println(content)
      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "application/json")
    }
  }
}