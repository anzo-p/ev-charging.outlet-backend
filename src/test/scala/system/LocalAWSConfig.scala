package system

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import zio.aws.core.config.{AwsConfig, CommonAwsConfig}
import zio.aws.netty.NettyHttpClient
import zio.{ZIO, ZLayer}

object LocalAWSConfig {

  lazy val credentials =
    StaticCredentialsProvider.create(AwsBasicCredentials.create("access-key-to-dynamodb-local", "mellon"))

  lazy val credentialsConfig: ZLayer[Any, Nothing, CommonAwsConfig] =
    ZLayer.scoped {
      ZIO.succeed(
        CommonAwsConfig(
          region              = None,
          credentialsProvider = credentials,
          endpointOverride    = None,
          commonClientConfig  = None
        ))
    }

  val awsConfig =
    NettyHttpClient.default ++ credentialsConfig >>> AwsConfig.configured()
}
