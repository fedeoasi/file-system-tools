import sbt._

object Dependencies {
  lazy val airframeLog = "org.wvlet.airframe" %% "airframe-log" % "0.52"
  lazy val arm = "com.jsuereth" %% "scala-arm" % "2.0"

  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.5.14"
  lazy val akkaStreamTest = "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.14" % Test

  lazy val mockito = "org.mockito" % "mockito-all" % "1.8.4" % Test
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.3" % Test
  lazy val commonsIo = "commons-io" % "commons-io" % "2.4"
  lazy val commonsCodec = "commons-codec" % "commons-codec" % "1.10"
  lazy val scalaCsv = "com.github.tototoshi" %% "scala-csv" % "1.3.5"
  lazy val scopt = "com.github.scopt" %% "scopt" % "3.7.0"
  lazy val sparkCore = "org.apache.spark" %% "spark-core" % "2.2.1"
}
