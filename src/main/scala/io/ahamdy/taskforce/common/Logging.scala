package io.ahamdy.taskforce.common

import monix.eval.Task
import com.typesafe.scalalogging.StrictLogging


trait Logging extends StrictLogging{
  def logDebug(msg: => String): Task[Unit] =
    Task(logger.debug(msg))

  def logError(msg: => String): Task[Unit] =
    Task(logger.error(msg))

  def logInfo(msg: => String): Task[Unit] =
    Task(logger.info(msg))

  def logWarning(msg: => String): Task[Unit] =
    Task(logger.warn(msg))

  def logTrace(msg: => String): Task[Unit] =
    Task(logger.trace(msg))
}
