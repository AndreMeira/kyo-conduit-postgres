package kyo

// In package `kyo` because reading through another column needs its package-private `read`.

/**
 * Builds kyo-sql column codecs from existing ones.
 */
object SqlSchemaInterop:

  /**
   * A column storing `A` the way `inner` stores `B`.
   *
   * @param inner the column codec of the stored representation
   * @param to converts a stored value to `A` once read
   * @param from converts `A` to its stored representation before writing
   * @tparam A the type the column holds
   * @tparam B the stored representation
   * @return the column codec for `A`
   */
  def biMap[A, B](inner: SqlSchema.Column[B])(to: B => A, from: A => B): SqlSchema.Column[A] =
    SqlSchema.of[A]((value, writer) => inner.write(from(value), writer), reader => to(inner.read(reader)))
