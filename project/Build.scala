import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "ReactiveCouchbase-play"
  val appVersion      = "0.1-SNAPSHOT"
  val appScalaVersion = "2.10.2"
  val appScalaBinaryVersion = "2.10"
  val appScalaCrossVersions = Seq("2.10.2")

  val local: Project.Initialize[Option[sbt.Resolver]] = version { (version: String) =>
    val localPublishRepo = "./repository"
    if(version.trim.endsWith("SNAPSHOT"))
      Some(Resolver.file("snapshots", new File(localPublishRepo + "/snapshots")))
    else Some(Resolver.file("releases", new File(localPublishRepo + "/releases")))
  }

  lazy val baseSettings = Defaults.defaultSettings ++ Seq(
    scalaVersion := appScalaVersion,
    scalaBinaryVersion := appScalaBinaryVersion,
    crossScalaVersions := appScalaCrossVersions,
    parallelExecution in Test := false
  )

  lazy val root = Project("root", base = file("."))
    .settings(baseSettings: _*)
    .settings(
      publishLocal := {},
      publish := {}
    ).aggregate(
      plugin,
      N1QLSample,
      ESSample,
      PersonsSample,
      ShortURLsSample,
      JavaShortUrlsSample
    )

  lazy val plugin = Project(appName, base = file("plugin"))
    .settings(baseSettings: _*)
    .settings(
      resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      resolvers += "Spy Repository" at "http://files.couchbase.com/maven2",
      libraryDependencies += "org.reactivecouchbase" %% "reactivecouchbase-core" % "0.1-SNAPSHOT",
      libraryDependencies += "org.reactivecouchbase" %% "reactivecouchbase-es" % "0.1-SNAPSHOT",
      libraryDependencies += "com.typesafe.play" %% "play" % "2.2.0" % "provided",
      libraryDependencies += "com.typesafe.play" %% "play-cache" % "2.2.0",
      libraryDependencies += "com.typesafe.play" %% "play-test" % "2.2.0" % "test",
      organization := "org.reactivecouchbase",
      version := appVersion,
      publishTo <<= local,
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { _ => false }
    )

    lazy val N1QLSample = play.Project(
      "scala-n1ql-sample",
      path = file("samples/scala/n1ql")
    ).settings(
      scalaVersion := appScalaVersion,
      scalaBinaryVersion := appScalaBinaryVersion,
      crossScalaVersions := appScalaCrossVersions,
      crossVersion := CrossVersion.full,
      parallelExecution in Test := false,
      publishLocal := {},
      publish := {}
    ).dependsOn(plugin)

    lazy val ESSample = play.Project(
      "scala-es-sample",
      path = file("samples/scala/orders")
    ).settings(
      scalaVersion := appScalaVersion,
      scalaBinaryVersion := appScalaBinaryVersion,
      crossScalaVersions := appScalaCrossVersions,
      crossVersion := CrossVersion.full,
      parallelExecution in Test := false,
      publishLocal := {},
      publish := {},
      libraryDependencies += "com.typesafe.play" %% "play-java" % "2.2.0" % "provided"
    ).dependsOn(plugin)

    lazy val PersonsSample = play.Project(
      "scala-persons-sample",
      path = file("samples/scala/persons")
    ).settings(
      scalaVersion := appScalaVersion,
      scalaBinaryVersion := appScalaBinaryVersion,
      crossScalaVersions := appScalaCrossVersions,
      crossVersion := CrossVersion.full,
      parallelExecution in Test := false,
      publishLocal := {},
      publish := {}
    ).dependsOn(plugin)

    lazy val ShortURLsSample = play.Project(
      "scala-shorturls-sample",
      path = file("samples/scala/shorturls")
    ).settings(
      scalaVersion := appScalaVersion,
      scalaBinaryVersion := appScalaBinaryVersion,
      crossScalaVersions := appScalaCrossVersions,
      crossVersion := CrossVersion.full,
      parallelExecution in Test := false,
      publishLocal := {},
      publish := {}
    ).dependsOn(plugin)

    lazy val JavaShortUrlsSample = play.Project(
      "java-shorturls-sample",
      path = file("samples/java/shorturls")
    ).settings(
      scalaVersion := appScalaVersion,
      scalaBinaryVersion := appScalaBinaryVersion,
      crossScalaVersions := appScalaCrossVersions,
      crossVersion := CrossVersion.full,
      parallelExecution in Test := false,
      publishLocal := {},
      publish := {},
      libraryDependencies += "com.typesafe.play" %% "play-java" % "2.2.0" % "provided",
      libraryDependencies += "org.reactivecouchbase" %% "reactivecouchbase-es" % "0.1-SNAPSHOT"
    ).dependsOn(plugin)
}
