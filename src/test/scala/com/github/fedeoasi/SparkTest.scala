package com.github.fedeoasi

import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterAll, Suite}

trait SparkTest extends BeforeAndAfterAll {
  this: Suite =>

  private var _sparkContext: SparkContext = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val conf = new SparkConf().setAppName("Find Identical Folders").setMaster("local[*]")
    _sparkContext = new SparkContext(conf)
  }

  override protected def afterAll(): Unit = {
    _sparkContext.stop()
    super.afterAll()
  }

  def sparkContext: SparkContext = _sparkContext
}
