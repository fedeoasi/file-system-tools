package com.github.fedeoasi.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.fedeoasi.output.Logging

object StreamUtils extends Logging {
  def withMaterializer[T](systemName: String)(f: ActorMaterializer => T): T = {
    implicit val system: ActorSystem = ActorSystem(systemName)
    try {
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      f(materializer)
    } finally {
      info(s"shutting down actor system ${system.name}")
      system.terminate()
    }
  }
}
