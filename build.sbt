name := "basic-project"

organization := "example"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.11.8","2.12.1")

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
)
routesGenerator := InjectedRoutesGenerator
lazy val root = (project in file(".")).enablePlugins(PlayScala)
