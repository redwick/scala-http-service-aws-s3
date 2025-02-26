import scala.collection.Seq

ThisBuild / version := "latest"

ThisBuild / scalaVersion := "2.13.15"

enablePlugins(JavaServerAppPackaging, DockerPlugin)

dockerExposedPorts := Seq(5055)
dockerRepository := Some("cr.selcloud.ru/marine-solutions")
dockerBaseImage := "registry.access.redhat.com/ubi9/openjdk-21:1.20-2.1721207866"

lazy val root = (project in file("."))
  .settings(
    name := "http-service"
  )

scalacOptions += "-Ymacro-annotations"


val PekkoVersion = "1.0.3"
val PekkoHttpVersion = "1.0.1"
val AkkaHttpCors = "1.2.0"
val SLF4JVersion = "2.0.13"
val SlickVersion = "3.5.1"
val PostgresSQLVersion = "42.7.3"
val MongoDBVersion = "4.10.0"
val CirceVersion = "0.14.9"
val AWSSDK = "1.12.765"
val ApachePoi = "5.3.0"
val SimpleJavaMailVersion = "8.11.2"
val JavaWebSocket = "1.6.0"

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
  "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
  "org.apache.pekko" %% "pekko-slf4j" % PekkoVersion,
  "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
  "org.apache.pekko" %% "pekko-http-cors" % PekkoHttpVersion,
  "org.slf4j" % "slf4j-api" % SLF4JVersion,
  "org.slf4j" % "slf4j-simple" % SLF4JVersion,
  "io.circe" %% "circe-core" % CirceVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-parser" % CirceVersion,
  "com.typesafe.slick" %% "slick" % SlickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
  "org.postgresql" % "postgresql" % PostgresSQLVersion,
  "com.amazonaws" % "aws-java-sdk-core" % AWSSDK,
  "com.amazonaws" % "aws-java-sdk-s3" % AWSSDK,
  "org.apache.poi" % "poi" % ApachePoi,
  "org.apache.poi" % "poi-ooxml" % ApachePoi,
  "org.simplejavamail" % "simple-java-mail" %  SimpleJavaMailVersion,
  "org.java-websocket" % "Java-WebSocket" % JavaWebSocket
)
