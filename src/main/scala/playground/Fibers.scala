package playground

import kyo.*
import scala.concurrent.duration.given

object Fibers extends KyoApp {

  def produce(channel: Channel[Int]): Unit < (Async & Abort[Closed]) =
    channel.put(42) <* Kyo.sleep(100.millis)

  def consume(channel: Channel[Int]): Unit < (Async & Abort[Closed]) =
    channel.drainUpTo(100).map(Console.printLine) <* Kyo.sleep(2.seconds)

  run {
    for {
      channel <- Channel.init[Int](100)
      p       <- produce(channel).forever.fork
      c       <- consume(channel).forever.fork
      _       <- p.get
      _       <- c.get
    } yield ()
  }
}
