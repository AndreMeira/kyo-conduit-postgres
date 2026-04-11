package com.augustnagro.magnum

import java.sql.{ Connection, PreparedStatement }

/**
 * Provides interoperability utilities for working with Magnum's database abstractions.
 */
object MagnumInterop {

  /**
   * Creates a DbTx instance from a given JDBC Connection.
   *
   * @param connection the JDBC Connection to use for database operations
   * @return a DbTx instance that can be used to execute SQL queries and updates
   */
  def makeDbTx(connection: Connection): DbTx =
    DbTx(connection, SqlLogger.Default)

  /**
   * Combines two SQL fragments with a specified separator, ensuring that empty fragments are handled gracefully.
   *
   * @param left the left SQL fragment
   * @param right the right SQL fragment
   * @param separator the string to separate the two fragments (e.g., " AND ", " OR ", ", ")
   * @return a new Frag that represents the combination of the two fragments with the separator
   */
  def combine(left: Frag, right: Frag, separator: String): Frag = {
    val nonEmpty = List(left, right).filter(_.sqlString.nonEmpty)
    Frag(
      sqlString = nonEmpty.iterator.map(_.sqlString).mkString(separator),
      params = nonEmpty.flatMap(_.params),
      writer = (statement, pos) =>
        nonEmpty.foldLeft(pos) { (currentPos, frag) =>
          frag.writer.write(statement, currentPos)
        },
    )
  }

  /**
   * Extension methods for Frag to enable fluent combination of SQL fragments
   * using AND, OR, comma, and space separators.
   * Also includes a method to conditionally include a fragment based on a Boolean condition.
   */
  object syntax {

    /**
     * An empty SQL fragment that can be used as a base case for combining fragments.
     * It has an empty SQL string, no parameters, and a writer that does nothing.
     *
     * @return an empty Frag instance
     */
    def emptyFragment: Frag =
      Frag(sqlString = "", params = Nil, writer = (statement, pos) => pos)

    extension (left: Frag)
      /**
       * Combines this fragment with another using " AND " as a separator
       *
       * @param right the right SQL fragment to combine with this fragment
       * @return a new Frag that represents the combination of this fragment and the right fragment
       */
      def and(right: Frag): Frag = combine(left, right, " AND ")

      /**
       * Combines this fragment with another using " OR " as a separator
       *
       * @param right the right SQL fragment to combine with this fragment
       * @return a new Frag that represents the combination of this fragment and the right fragment
       */
      def or(right: Frag): Frag = combine(left, right, " OR ")

      /**
       * Combines this fragment with another using ", " as a separator
       *
       * @param right the right SQL fragment to combine with this fragment
       * @return a new Frag that represents the combination of this fragment and the right fragment
       */
      def comma(right: Frag): Frag = combine(left, right, ", ")

      /**
       * Combines this fragment with another using a single space as a separator
       *
       * @param another the right SQL fragment to combine with this fragment
       * @return a new Frag that represents the combination of this fragment and the another fragment
       */
      def ++(another: Frag): Frag = combine(left, another, " ")

      /**
       * Combines this fragment with another using a custom separator.
       *
       * @param separator the string to separate the two fragments (e.g., " AND ", " OR ", ", ")
       * @param right the right SQL fragment to combine with this fragment
       * @return a new Frag that represents the combination of this fragment and the right fragment
       */
      def concat(separator: String)(right: Frag): Frag = combine(left, right, " ")

      /**
       * Conditionally combines this fragment with another fragment based on a Boolean condition.
       *
       * @param condition the Boolean condition that determines whether to include the other fragment
       * @param other the SQL fragment to conditionally combine with this fragment if the condition is true
       * @return a new Frag that represents the combination of this fragment
       *         and the other fragment if the condition is true,
       *         or just this fragment if the condition is false
       */
      def when(condition: Boolean)(other: => Frag): Frag = if (condition) left ++ other else left
  }
}
