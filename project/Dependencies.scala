import sbt._

object Dependencies {
  lazy val arm = "com.jsuereth" %% "scala-arm" % "2.0"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.3"
  lazy val commonsIo = "commons-io" % "commons-io" % "2.4"
  lazy val commonsCodec = "commons-codec" % "commons-codec" % "1.10"
  lazy val scalaCsv = "com.github.tototoshi" %% "scala-csv" % "1.3.5"
  lazy val sparkCore = "org.apache.spark" %% "spark-core" % "2.2.1"
}
