import es.Bootstrap
import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Bootstrap.bootstrap()
    Logger.info("Application has started")
  }

  override def onStop(app: Application) {
    Bootstrap.shutdown()
    Logger.info("Application shutdown...")
  }

}