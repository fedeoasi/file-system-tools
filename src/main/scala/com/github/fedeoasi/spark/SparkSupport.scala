package com.github.fedeoasi.spark

import org.apache.spark.{SparkConf, SparkContext}

trait SparkSupport {
  def withSparkContext[T](f: SparkContext => T): T = {
    val sc = newContext
    try f(sc) finally sc.stop
  }

  private def newContext: SparkContext = {
    val conf = new SparkConf()
      .setAppName("Find Identical Folders")
      .setMaster("local[*]")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    new SparkContext(conf)
  }
}
