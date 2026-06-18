package playground.workflow

import kyo.*

/**
 * A workflow is a sequence of steps that can be executed asynchronously.
 * Each step can produce an output that can be used as input for the initialise step.
 * The workflow can be paused and resumed at any point,
 * allowing for long-running processes that can be interrupted and continued later.
 *
 * The Workflow trait defines the logic of the workflow,
 * which takes an input and a state and produces a new state and a result.
 * The result can either be a continuation with a new input or a final output.
 * The execute method runs the workflow by repeatedly calling the logic until it produces a final output.
 */
trait Workflow[In, Out, State]:

  /**
   * The logic of the workflow, which takes an input and a state and produces a new state and a result.
   * The result can either be a continuation with a new input or a final output.
   */
  def logic: (In, State) => (State, Workflow.Result[In, Out]) < Async

  /**
   * Executes the workflow by repeatedly calling the logic until it produces a final output.
   * It takes an initial input and an initial state, and returns the final state and output.
   */
  def execute(input: In, initialState: State): (State, Out) < Async =
    logic(input, initialState).map:
      case state -> Workflow.Result.Continue(nextInput) => execute(nextInput, state)
      case state -> Workflow.Result.Done(output)        => state -> output

/**
 * The Workflow object defines the Result type, which can either be a continuation with a new input or a final output.
 * The Continue case class represents a continuation with a new input, while the Done case class represents a final output.
 */
object Workflow:

  /**
   * The continue method creates a Result that represents a continuation with a new input.
   */
  def continue[In](input: In): Result[In, Nothing] = Result.Continue(input)
  
  /**
   * The done method creates a Result that represents a final output.
   */
  def done[Out](output: Out): Result[Nothing, Out] = Result.Done(output)

  /**
   * The Result type represents the outcome of a step in the workflow.
   */
  enum Result[+In, +Out]:
    case Continue(input: In)
    case Done(output: Out)

  /**
   * A simple workflow implementation that defines the initialise step and the continuation logic separately.
   * The initialise method takes an input and produces a new state, while the transition method takes a state and produces a result.
   * The execute method runs the workflow by calling the initialise method to get the initial state,
   * and then repeatedly calling the transition method until it produces a final output.
   * This allows for a clear separation of concerns between the state transitions and the workflow logic.
   */
  trait Simple[State, Out]:

    /**
     * The transition method takes a state and produces a result.
     * This method is responsible for determining the initialise step of the workflow based on the current state,
     * and can either produce a continuation with a new input or a final output.
     */
    def transition: State => Result[State, Out] < Async

    /**
     * Executes the workflow by calling the initialise method to get the initial state,
     * and then repeatedly calling the transition method until it produces a final output.
     * This allows for a clear separation of concerns between the state transitions and the workflow logic.
     */
    def execute(initial: State): Out < Async =
      transition(initial).map:
        case Result.Continue(state) => execute(state)
        case Result.Done(output)    => output

  /**
   * A more complex workflow implementation that defines the next step of the workflow as a continuation of the current step.
   * The next method produces a result that can either be a continuation with a new input or a final output.
   * The execute method runs the workflow by repeatedly calling the next method until it produces a final output.
   * This allows for a more flexible workflow design, where each step can determine the next step based on the current state
   * and input, without needing to define a separate initialise method or transition method.
   */
  trait Behaviour[Out]:

    /**
     * The next method produces a result that can either be a continuation with a new input or a final output.
     * This method is responsible for determining the next step of the workflow based on the current state and input,
     * and can either produce a continuation with a new input or a final output.
     */
    def next: Result[Behaviour[Out], Out] < Async

    /**
     * Executes the workflow by repeatedly calling the next method until it produces a final output.
     * This allows for a more flexible workflow design, where each step can determine the next step based on the current state
     * and input, without needing to define a separate initialise method or transition method.
     */
    def execute: Out < Async =
      next.map:
        case Result.Continue(next) => next.execute
        case Result.Done(output)   => output

  /**
   * The Workflow object also provides helper methods for creating instances of the Simple and Behaviour traits.
   * The apply method for the Simple trait takes a logic function that defines the transition logic of the workflow, 
   * and returns an instance of the Simple trait that implements the execute method using the provided logic
   */
  object Behaviour:
    def apply[Out](logic: => Result[Behaviour[Out], Out] < Async): Behaviour[Out] = new:
      def next: Result[Behaviour[Out], Out] < Async = logic
