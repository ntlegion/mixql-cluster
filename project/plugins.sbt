addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.6")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.10"
)