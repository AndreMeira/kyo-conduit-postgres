package conduit.domain.error

/**
 * Base trait for all application errors in the Conduit application.
 *
 * This trait serves as the root error type for the entire application, providing
 * a standardized way to handle and communicate errors across different layers.
 * All application-specific errors should extend this trait directly or one of
 * its subtypes.
 *
 * Each error provides a human-readable message and a kind identifier for
 * categorization and logging purposes.
 */
trait ApplicationError:
  /**
   * Returns a human-readable message describing the error.
   *
   * @return the error message
   */
  def message: String

  /**
   * Returns the kind/type of error based on the class name.
   *
   * This is used for categorization and can be useful in error handling
   * and logging scenarios.
   *
   * @return the error kind as a string derived from the class name
   */
  def kind: String = getClass.getSimpleName

  /**
   * Returns a string representation of the error.
   *
   * @return a formatted string containing the error kind and message
   */
  override def toString: String = s"{kind: $kind, message: $message}"

object ApplicationError:
  /**
   * Trait for domain-specific errors.
   *
   * Domain errors represent business logic violations and constraint failures
   * that occur during normal application operation. These errors are typically
   * recoverable and expected as part of the application's domain logic.
   */
  trait DomainError extends ApplicationError

  /**
   * Trait for transient errors.
   *
   * Transient errors represent temporary issues that may be resolved by retrying
   * the operation at a later time. Examples include temporary network failures
   * or resource unavailability.
   */
  trait TransientError extends ApplicationError

  /**
   * Trait for conflict errors.
   *
   * Conflict errors represent situations where an operation violates a constraint,
   * such as attempting to create a resource that already exists or duplicate key
   * violations. Extends DomainError.
   */
  trait ConflictError extends DomainError

  /**
   * Trait for unauthorized errors.
   *
   * Unauthorized errors represent authentication or authorization failures,
   * where a user lacks the necessary credentials or permissions to perform
   * an operation. Extends DomainError.
   */
  trait UnauthorisedError extends DomainError

  /**
   * Trait for inconsistent state errors.
   *
   * Inconsistent state errors represent situations where the application's
   * internal state is in an unexpected or invalid condition, typically indicating
   * a bug or data corruption. Extends DomainError.
   */
  trait InconsistentState extends DomainError

  /**
   * Trait for not found errors.
   *
   * Not found errors represent situations where a requested resource cannot be
   * found in the system. Extends DomainError.
   */
  trait NotFoundError extends DomainError

  /**
   * Trait for vendor-specific errors.
   *
   * Vendor errors represent issues that arise from interactions with external
   * systems or libraries, such as database errors, network errors, or third-party
   * API failures. These errors may require special handling based on the vendor's
   * error semantics.
   */
  trait VendorError extends ApplicationError

  /**
    * Trait for unrecoverable errors.
    *
    * Unrecoverable errors represent critical failures that cannot be handled or
    * recovered from within the application. These errors typically indicate
    * severe issues such as data corruption, critical bugs, or system failures
    * that require immediate attention and may necessitate shutting down the
    * application to prevent further damage.
    */
  trait UnrecoverableError extends ApplicationError
