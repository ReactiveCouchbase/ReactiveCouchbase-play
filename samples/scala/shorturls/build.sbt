name := "shorturls"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  cache,
  ws, 
  "org.reactivecouchbase" %% "reactivecouchbase-play" % "0.3-SNAPSHOT"
)

resolvers += "ReactiveCouchbase" at "https://raw.github.com/ReactiveCouchbase/repository/master/snapshots"