package com.github.fedeoasi.cli

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CliCommandTest extends AnyFlatSpec with Matchers {
  "CliCommand" should "output valid string" in {
    val command = CliCommand("0123456789001234567890123", "desc")
    command.toString should be("0123456789001234567890123  desc")
  }

  it should "handle longer names properly" in {
    val command = CliCommand("012345678901234567890123456789", "desc")
    command.toString should be("012345678901234567890123456789  desc")
  }

  it should "handle shorter names properly" in {
    val command = CliCommand("0123456789", "desc")
    command.toString should be("0123456789               desc")
  }
}
