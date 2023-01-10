package outlet.backend.system

import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import software.amazon.awssdk.regions.Region
import zio.aws.core.config.AwsConfig
import zio.aws.dynamodb.DynamoDb
import zio.{ULayer, ZIO, ZLayer}

import java.io.File
import java.net.URI

object LocalDynamoDB {

  import zio.testcontainers._

  lazy val dockerCompose: ULayer[DockerComposeContainer] = ZLayer.fromTestContainer {
    new DockerComposeContainer(
      new File("docker-compose.yml"),
      List(
        ExposedService("dynamodb-local", 8000)
      )
    )
  }

  lazy val container: ZLayer[DockerComposeContainer, Nothing, Unit] =
    ZLayer.fromZIO {
      for {
        docker <- ZIO.service[DockerComposeContainer]
        _      <- docker.getHostAndPort("dynamodb-local")(8000)
      } yield ()
    }

  lazy val dynamoDB: ZLayer[AwsConfig, Throwable, DynamoDb] =
    DynamoDb.customized { builder =>
      builder.endpointOverride(URI.create("http://localhost:8000")).region(Region.EU_WEST_1)
    }

  val layer: ZLayer[Any with AwsConfig, Throwable, DynamoDb] =
    dockerCompose >>> container >>> dynamoDB
}
