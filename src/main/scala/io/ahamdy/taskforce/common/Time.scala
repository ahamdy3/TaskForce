package io.ahamdy.taskforce.common

import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.IO


trait Time {
  def now: IO[ZonedDateTime] = IO(unsafeNow())
  def epoch: ZonedDateTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))
  def unsafeNow(): ZonedDateTime
}

object TimeImpl extends Time {

  override def unsafeNow(): ZonedDateTime =
    ZonedDateTime.now.withZoneSameInstant(ZoneId.of("UTC"))

}

