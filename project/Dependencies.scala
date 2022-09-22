import sbt._

object Version {
  val zio        = "2.0.1"
  val zioLogging = "2.1.0"
}

object Dependencies {

  val libraryDependencies = Seq(
    "dev.zio" %% "zio"               % Version.zio,
    "dev.zio" %% "zio-logging-slf4j" % Version.zioLogging,
    "dev.zio" %% "zio-logging"       % Version.zioLogging
  )
}
