package com.github.fedeoasi.cli

case class CliCommand(name: String, description: String) {
  private val MAX_COMMAND_NAME_LENGTH = 23
  override def toString: String = {
    val spacer = "  " + " " * Math.max(0, MAX_COMMAND_NAME_LENGTH - name.length)
    name + spacer + description
  }
}
