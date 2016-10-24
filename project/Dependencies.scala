import sbt._

object Dependencies {

  val funCqrsVersion = "0.4.8"

  lazy val appDeps = {
    Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      // Fun.CQRS
      "io.strongtyped" %% "fun-cqrs-akka"      % funCqrsVersion,
      "io.strongtyped" %% "fun-cqrs-play-json" % funCqrsVersion,
      // LevelDB
      "org.iq80.leveldb"          % "leveldb"        % "0.7",
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
      // Macwire
      "com.softwaremill.macwire" %% "macros" % "2.2.5"
    )
  }

  lazy val testDeps =
    Seq(
      "io.strongtyped"         %% "fun-cqrs-test-kit"  % funCqrsVersion % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1"        % Test,
      "org.scalatest"          %% "scalatest"          % "3.0.0"        % Test
    )

}
