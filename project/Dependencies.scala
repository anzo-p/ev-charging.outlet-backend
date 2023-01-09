import sbt._

object Dependencies {
  val chQosLogBack = "1.4.5"
  val zio          = "2.0.5"
  val zioDynamoDB  = "0.2.0-RC2"
  val zioHttp      = "2.0.0-RC11"
  val zioJson      = "0.3.0"
  val zioKinesis   = "0.30.1"
  val zioLogging   = "2.1.6"
  val zioParser    = "0.1.7"
  val zioSqs       = "0.5.0"

  val libraryDependencies: Seq[ModuleID] = Seq(
    "dev.zio"                 %% "zio"                % zio,
    "dev.zio"                 %% "zio-dynamodb"       % zioDynamoDB,
    "io.d11"                  %% "zhttp"              % zioHttp,
    "dev.zio"                 %% "zio-json"           % zioJson,
    "nl.vroste"               %% "zio-kinesis"        % zioKinesis,
    "dev.zio"                 %% "zio-logging"        % zioLogging,
    "dev.zio"                 %% "zio-logging-slf4j"  % zioLogging,
    "dev.zio"                 %% "zio-parser"         % zioParser,
    "dev.zio"                 %% "zio-sqs"            % zioSqs,
    "dev.zio"                 %% "zio-test"           % zio % "test",
    "dev.zio"                 %% "zio-test-sbt"       % zio % "test",
    "dev.zio"                 %% "zio-mock"           % "1.0.0-RC9",
    "ch.qos.logback"          % "logback-classic"     % chQosLogBack,
    "com.thesamet.scalapb"    %% "scalapb-runtime"    % scalapb.compiler.Version.scalapbVersion % "protobuf",
    "com.anzop"               %% "ev-charger-shared"  % "0.0.1",
    "software.amazon.awssdk"  % "dynamodb"            % "2.17.166",
    "com.amazonaws"           % "DynamoDBLocal"       % "1.17.0" % "it,test",
    "com.github.sideeffffect" %% "zio-testcontainers" % "0.4.1" % Test
  )
}
