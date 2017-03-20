name := "basic-project"

organization := "example"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.11.8","2.12.1")

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(
  "org.scaldi" % "scaldi_2.12" % "0.5.8",
  "org.webjars.bower" % "phaser" % "2.4.4",
  "com.github.webjars" % "webjars-play" % "v2.6.0-M1",
  "org.scalaz" % "scalaz-core_2.12" % "7.3.0-M10",
  "com.github.nscala-time" %% "nscala-time" % "2.16.0",
  "io.reactivex" %% "rxscala" % "0.26.5",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
)
routesGenerator := InjectedRoutesGenerator
lazy val root = (project in file(".")).enablePlugins(PlayScala)
