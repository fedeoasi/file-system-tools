import sbt._

object Dependencies {
  lazy val airframeLog = "org.wvlet.airframe" %% "airframe-log" % "22.4.2"

  private val akkaStreamVersion = "2.6.19"
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion
  lazy val akkaStreamTest = "com.typesafe.akka" %% "akka-stream-testkit" % akkaStreamVersion % Test

  lazy val arm = "com.michaelpollmeier" %% "scala-arm" % "2.1"
  lazy val mockito = "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.11" % Test
  lazy val commonsIo = "commons-io" % "commons-io" % "2.4"
  lazy val commonsCodec = "commons-codec" % "commons-codec" % "1.10"
  lazy val scalaCsv = "com.github.tototoshi" %% "scala-csv" % "1.3.10"
  lazy val scopt = "com.github.scopt" %% "scopt" % "4.0.1"
  lazy val sparkCore = "org.apache.spark" %% "spark-core" % "3.2.1"
  lazy val scalaRetry = "com.github.hipjim" %% "scala-retry" % "0.2.4"
}
