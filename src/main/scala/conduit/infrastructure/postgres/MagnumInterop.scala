package com.augustnagro.magnum

import java.sql.Connection

object MagnumInterop {
  def makeDbTx(connection: Connection): DbTx =
    DbTx(connection, SqlLogger.Default)
}
