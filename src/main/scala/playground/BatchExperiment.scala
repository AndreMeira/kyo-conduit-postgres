package playground

import kyo.*

object BatchExperiment extends KyoApp {

  // Using 'Batch.sourceSeq' for processing the entire sequence at once, returning a 'Seq'
  val source1 = Batch.sourceSeq[Int, String, Any] { seq =>
    seq.map(i => i.toString)
  }

  // Using 'Batch.sourceMap' for processing the entire sequence at once, returning a 'Map'
  val source2 = Batch.sourceMap[Int, String, Sync] { seq =>
    // Source functions can perform arbitrary effects like 'Sync' before returning the results
    Sync.defer {
      seq.map(i => i -> i.toString).toMap
    }
  }

  // Using 'Batch.source' for individual effect suspensions
  // This is a more generic method that allows effects for each of the inputs
  val source3 = Batch.source[Int, String, Sync] { seq =>
    val map = seq.map { i =>
      i -> Sync.defer((i * 2).toString)
    }.toMap
    (i: Int) => map(i)
  }

  // Example usage
  val result =
    for
      a: Int <- Batch.eval(Seq(1, 2, 3))
      b1     <- source1(a)
      b2     <- source2(a)
      b3     <- source3(a)
    yield (a, b1, b2, b3)

  // Handle the effect
  val finalResult: Seq[(Int, String, String, String)] < Sync =
    Batch.run(result)

  run {
    for
      seq <- finalResult
      _   <- Console.printLine(s">>>>>>>>>>>>>>>>>>>>>> $seq")
    yield ()
  }

}
