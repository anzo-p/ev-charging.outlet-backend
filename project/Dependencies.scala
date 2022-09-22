import sbt._

object Dependencies {
  val chQosLogBack = "1.4.1"
  val zio          = "2.0.1"
  val zioLogging   = "2.1.0"

  val libraryDependencies = Seq(
    "dev.zio"        %% "zio"               % zio,
    "dev.zio"        %% "zio-logging"       % zioLogging,
    "dev.zio"        %% "zio-logging-slf4j" % zioLogging,
    "ch.qos.logback" % "logback-classic"    % chQosLogBack
  )
}
