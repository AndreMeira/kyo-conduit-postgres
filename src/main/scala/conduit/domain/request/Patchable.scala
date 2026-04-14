package conduit.domain.request

/** A type that represents a field value in a PATCH-style request:
  *   - '''Present(value)''' — the field was provided with a value (update it)
  *   - '''Absent''' — the field was not included in the JSON (leave unchanged)
  *   - '''Emtpy''' — the field was explicitly set to `null` (clear it)
  *
  * This is necessary because Circe's default `Option` decoding cannot
  * distinguish between a JSON `null` and a missing field — both become `None`.
  */
enum Patchable[+A]:
  case Present(value: A)
  case Absent
  case Emtpy

  def map[B](f: A => B): Patchable[B] = this match
    case Present(value) => Present(f(value))
    case Absent         => Absent
    case Emtpy          => Emtpy
