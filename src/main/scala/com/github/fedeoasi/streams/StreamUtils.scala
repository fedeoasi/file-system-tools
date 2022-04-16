package com.github.fedeoasi.streams

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.github.fedeoasi.output.Logging

object StreamUtils extends Logging {
  def withMaterializer[T](systemName: String)(f: Materializer => T): T = {
    implicit val system: ActorSystem = ActorSystem(systemName)
    try {
      implicit val materializer: Materializer = Materializer.createMaterializer(system)
      f(materializer)
    } finally {
      info(s"shutting down actor system ${system.name}")
      system.terminate()
    }
  }
}
