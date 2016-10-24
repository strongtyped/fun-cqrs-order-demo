import Dependencies._

name := """fun-cqrs-scalaio"""

version := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.8"

scalafmtConfig in ThisBuild := Some(file(".scalafmt.conf"))

libraryDependencies ++= Seq(ws) ++ appDeps ++ testDeps

lazy val app = (project in file(".")).enablePlugins(PlayScala)
