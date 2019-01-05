package com.github.fedeoasi.cli

trait CliAware {
  def command: CliCommand
  def main(args: Array[String]): Unit
}
