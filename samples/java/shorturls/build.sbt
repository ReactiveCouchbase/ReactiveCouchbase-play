name := "shorturls"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  cache,
  javaWs 
  "org.reactivecouchbase" %% "reactivecouchbase-play" % "0.3-SNAPSHOT"
)

resolvers += "ReactiveCouchbase" at "https://raw.github.com/ReactiveCouchbase/repository/master/snapshots"