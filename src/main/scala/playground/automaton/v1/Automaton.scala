package playground.automaton.v1

trait Player {
  def choice: Player.Choice
  def next(otherChoice: Player.Choice): Player
}

object Player:
  enum Choice:
    case Left, Right
