import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "persons"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    cache,
    "org.reactivecouchbase" %% "reactivecouchbase-play" % "0.1"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "ReactiveCouchbase" at "https://raw.github.com/ReactiveCouchbase/repository/master/snapshots"
  )

}
