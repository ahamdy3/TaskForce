package io.ahamdy.jobforce.common

import cats.Show
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import fs2._

trait Logging extends StrictLogging{
  def logDebug[A: Show](a: A): Task[Unit] =
    Task.delay(logger.debug(a.show))

  def logError[A: Show](a: A): Task[Unit] =
    Task.delay(logger.error(a.show))

  def logInfo[A: Show](a: A): Task[Unit] =
    Task.delay(logger.info(a.show))

  def logWarning[A: Show](a: A): Task[Unit] =
    Task.delay(logger.warn(a.show))

  def logTrace[A: Show](a: A): Task[Unit] =
    Task.delay(logger.trace(a.show))
}
