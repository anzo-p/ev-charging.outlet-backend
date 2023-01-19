import sbt._
import Settings._

resolvers += "DynamoDB Local Release Repository".at("https://s3-us-west-2.amazonaws.com/dynamodb-local/release")

val `app` = Project("ev-charging", file("."))
  .settings(commonSettings)
  .settings(organization := "com.anzop")
  .settings(name := "outlet-backend")
  .settings(version := "0.0.1")
  .settings(libraryDependencies ++= Dependencies.libraryDependencies)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

enablePlugins(JavaAppPackaging)
