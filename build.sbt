import Dependencies._

lazy val cliMain = "com.github.fedeoasi.cli.Run"

lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "com.github.fedeoasi",
      scalaVersion := "2.13.18",
      version      := "0.1.0-SNAPSHOT"
    )),
    Compile / packageBin / mainClass := Some(cliMain),
    Compile / run / mainClass := Some(cliMain),
    name := "File System Tools",
    libraryDependencies ++= Seq(
      airframeLog, akkaStream, akkaStreamTest, arm, commonsCodec, commonsIo, mockito, scalaCsv, scalaTest, scopt, sparkCore),
    dependencyOverrides += "io.netty" % "netty-handler" % "4.1.68.Final", // needed because spark-core 3.2.1 imports both 4.1.68.Final and 4.1.50.Final of different netty libraries
    Test / parallelExecution := false,
    scalacOptions ++= Seq(
      "-deprecation",
      "-Ywarn-unused",
      "-Xfatal-warnings"
    ),
  ).enablePlugins(JavaAppPackaging)
