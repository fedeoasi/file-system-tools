import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.github.fedeoasi",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "File System Tools",
    libraryDependencies ++= Seq(arm, commonsCodec, commonsIo, scalaCsv, scalaTest % Test)
  )
