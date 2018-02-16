package io.ahamdy.taskforce.common

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging


trait Logging extends StrictLogging{
  def logDebug(msg: String): IO[Unit] =
    IO(logger.debug(msg))

  def logError(msg: String): IO[Unit] =
    IO(logger.error(msg))

  def logInfo(msg: String): IO[Unit] =
    IO(logger.info(msg))

  def logWarning(msg: String): IO[Unit] =
    IO(logger.warn(msg))

  def logTrace(msg: String): IO[Unit] =
    IO(logger.trace(msg))
}
