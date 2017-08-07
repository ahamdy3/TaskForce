package io.ahamdy.taskforce.common

import com.typesafe.scalalogging.StrictLogging
import fs2._

trait Logging extends StrictLogging{
  def logDebug(msg: String): Task[Unit] =
    Task.delay(logger.debug(msg))

  def logError(msg: String): Task[Unit] =
    Task.delay(logger.error(msg))

  def logInfo(msg: String): Task[Unit] =
    Task.delay(logger.info(msg))

  def logWarning(msg: String): Task[Unit] =
    Task.delay(logger.warn(msg))

  def logTrace(msg: String): Task[Unit] =
    Task.delay(logger.trace(msg))
}
