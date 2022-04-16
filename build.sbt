import Dependencies._

lazy val cliMain = "com.github.fedeoasi.cli.CliMain"

lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "com.github.fedeoasi",
      scalaVersion := "2.13.8",
      version      := "0.1.0-SNAPSHOT"
    )),
    mainClass in (Compile, packageBin) := Some(cliMain),
    mainClass in (Compile, run) := Some(cliMain),
    name := "File System Tools",
    libraryDependencies ++= Seq(
      airframeLog, akkaStream, akkaStreamTest, arm, commonsCodec, commonsIo, mockito, scalaCsv, scalaTest, scopt, sparkCore),
    dependencyOverrides ++= Set(
      "io.netty" % "netty-handler" % "4.1.68.Final" // needed because spark-core 3.2.1 imports both 4.1.68.Final and 4.1.50.Final of different netty libraries
    ),
    parallelExecution in Test := false
  ).enablePlugins(JavaAppPackaging)
