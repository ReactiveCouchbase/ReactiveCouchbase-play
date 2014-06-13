import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "ReactiveCouchbase-play"
  val appVersion      = "0.3-SNAPSHOT"
  val appScalaVersion = "2.11.1"
  //val appScalaBinaryVersion = "2.10"
  val appScalaCrossVersions = Seq("2.11.1", "2.10.4")

  val local: Def.Initialize[Option[sbt.Resolver]] = version { (version: String) =>
    val localPublishRepo = "./repository"
    if(version.trim.endsWith("SNAPSHOT"))
      Some(Resolver.file("snapshots", new File(localPublishRepo + "/snapshots")))
    else Some(Resolver.file("releases", new File(localPublishRepo + "/releases")))
  }

  val nexusPublish = version { v =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  }

  lazy val baseSettings = Defaults.defaultSettings ++ Seq(
    scalaVersion := appScalaVersion,
    //scalaBinaryVersion := appScalaBinaryVersion,
    crossScalaVersions := appScalaCrossVersions,
    parallelExecution in Test := false
  )

  lazy val root = Project("root", base = file("."))
    .settings(baseSettings: _*)
    .settings(
      publishLocal := {},
      publish := {}
    ).aggregate(
      plugin
    )

  lazy val plugin = Project(appName, base = file("plugin"))
    .settings(baseSettings: _*)
    .settings(
      resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      libraryDependencies += "org.reactivecouchbase" %% "reactivecouchbase-core" % "0.3-SNAPSHOT",
      libraryDependencies += "org.reactivecouchbase" %% "reactivecouchbase-es" % "0.3-SNAPSHOT",
      libraryDependencies += "com.typesafe.play" %% "play" % "2.3.0" % "provided",
      libraryDependencies += "com.typesafe.play" %% "play-cache" % "2.3.0",
      libraryDependencies += "com.google.guava" % "guava" % "17.0",
      //libraryDependencies += "com.typesafe.play" %% "play-test" % "2.3.0" % "test",
      organization := "org.reactivecouchbase",
      version := appVersion,
      publishTo <<= local,
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { _ => false },
      pomExtra := (
        <url>http://reactivecouchbase.org</url>
          <licenses>
            <license>
              <name>Apache 2</name>
              <url>http://www.apache.org/licenses/LICENSE-2.0</url>
              <distribution>repo</distribution>
            </license>
          </licenses>
          <scm>
            <url>git@github.com:ReactiveCouchbase/ReactiveCouchbase-play.git</url>
            <connection>scm:git:git@github.com:ReactiveCouchbase/ReactiveCouchbase-play.git</connection>
          </scm>
          <developers>
            <developer>
              <id>mathieu.ancelin</id>
              <name>Mathieu ANCELIN</name>
              <url>https://github.com/mathieuancelin</url>
            </developer>
          </developers>)
    )
}
