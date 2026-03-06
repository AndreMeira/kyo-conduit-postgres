package playground

import kyo.*

object EffectList extends KyoApp {

  test("test 1")

  val effects: List[Unit < Sync] = List(
    Console.printLine("Hello, world!"),
    Console.printLine("Running tests..."),
    Console.printLine("Running tests... 2"),
  )

  def test(effect: String): Unit =
    run {
      Kyo.unit
        *> Console.printLine(s"Running test: $effect")
        *> effects.fold(Kyo.unit: Unit < Sync)((acc, eff) => acc *> eff)
        *> Console.printLine("Running test...")
        *> Kyo.sleep(1.second)
        *> Console.printLine("Test done!")
    }
}
