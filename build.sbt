import Dependencies._

lazy val cliMain = "com.github.fedeoasi.cli.CliMain"

lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "com.github.fedeoasi",
      scalaVersion := "2.12.15",
      version      := "0.1.0-SNAPSHOT"
    )),
    mainClass in (Compile, packageBin) := Some(cliMain),
    mainClass in (Compile, run) := Some(cliMain),
    name := "File System Tools",
    libraryDependencies ++= Seq(
      airframeLog, akkaStream, akkaStreamTest, arm, commonsCodec, commonsIo, mockito, scalaCsv, scalaTest, scopt, sparkCore),
    parallelExecution in Test := false
  ).enablePlugins(JavaAppPackaging)
