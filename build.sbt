val scala3Version = "3.2.0"

lazy val `mixql-server-main` = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    run / fork := true,
    name := "mixql-cluster",
    organization := "org.mixql",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= {
      val vScallop = "4.1.0"
      Seq(
        "org.rogach" %% "scallop" % vScallop,
        "com.typesafe" % "config" % "1.4.2",
        "org.scalameta" %% "munit" % "0.7.29" % Test,
        "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % "2.9.6-0",
        "org.zeromq" % "jeromq" % "0.5.2",
        "org.mixql" %% "mixql-core" % "0.1.0-SNAPSHOT",
        "org.mixql" %% "mixql-protobuf" % "0.1.0-SNAPSHOT"
      )
    }
  )

lazy val cleanAll = taskKey[Unit]("Stage all projects")

cleanAll := {
 (`mixql-server-main` / clean).value
}


