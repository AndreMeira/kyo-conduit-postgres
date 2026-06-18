package playground.workflow

import kyo.*
import playground.workflow.RegistrationWorkflow.*

class RegistrationWorkflow extends Workflow.Simple[State, Output] {

  /**
   * The transition method takes a state and produces a result.
   * This method is responsible for determining the initialise step of the workflow based on the current state,
   * and can either produce a continuation with a new input or a final output.
   */
  override def transition: State => Workflow.Result[State, Output] < Async =
    case state: State.Started => StartStage.exec(state)
    case state: State.Pending => ConfirmationStage.exec(state)
}

object RegistrationWorkflow:
  enum Output:
    case RegistrationConfirmed(userId: String)
    case Failure(reason: String)

  enum State:
    case Started(name: String, email: String)
    case Pending(name: String, email: String, code: String)

  object StartStage:
    def exec(state: State.Started): Workflow.Result[State, Output] < Sync = Kyo.defer:
      // Simulate sending a confirmation email and generating a code
      val code = "123456"
      println(s"Sending confirmation email to ${state.email} with code: $code")
      Workflow.continue(State.Pending(state.name, state.email, code))

  object ConfirmationStage:
    def exec(state: State.Pending): Workflow.Result[State, Output] < Sync = Kyo.defer:
      val userId = java.util.UUID.randomUUID().toString
      println(s"Code confirmed for ${state.email}. User ID: $userId")
      Workflow.done(Output.RegistrationConfirmed(userId))
