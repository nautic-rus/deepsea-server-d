import scala.collection.Seq

ThisBuild / version := "initial"

ThisBuild / scalaVersion := "2.13.14"

enablePlugins(JavaServerAppPackaging, DockerPlugin)

dockerExposedPorts := Seq(5055)
dockerRepository := Some("files")
//dockerExposedVolumes := Seq("/opt/docker/logs")
dockerRepository := Some("iamdinarahello") //тут мое имя из 5докера

lazy val root = (project in file("."))
  .settings(
    name := "deepsea-serv"
  )
scalacOptions += "-Ymacro-annotations"

val PekkoVersion = "1.0.2"
val PekkoHttpVersion = "1.0.1"
val AkkaHttpCors = "1.2.0"
val SLF4JVersion = "2.0.13"
val SlickVersion = "3.5.1"
val PostgresSQLVersion = "42.7.3"
val OracleDBVersion = "21.6.0.0.1"
val MongoDBVersion = "4.10.0"
val CirceVersion = "0.14.7"
val ITextVersion = "8.0.0"

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
  "com.oracle.database.jdbc.debug" % "ojdbc8_g" % OracleDBVersion,
  "com.itextpdf" % "itext7-core" % ITextVersion
)
