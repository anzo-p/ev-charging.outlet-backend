import sbt._

object Dependencies {
  val chQosLogBack = "1.4.5"
  val zio          = "2.0.5"
  val zioHttp      = "2.0.0-RC11"
  val zioJson      = "0.3.0"
  val zioKinesis   = "0.30.1"
  val zioLogging   = "2.1.5"

  val libraryDependencies = Seq(
    "dev.zio"        %% "zio"               % zio,
    "dev.zio"        %% "zio-json"          % zioJson,
    "nl.vroste"      %% "zio-kinesis"       % zioKinesis,
    "dev.zio"        %% "zio-logging"       % zioLogging,
    "dev.zio"        %% "zio-logging-slf4j" % zioLogging,
    "io.d11"         %% "zhttp"             % zioHttp,
    "ch.qos.logback" % "logback-classic"    % chQosLogBack
  )
}
