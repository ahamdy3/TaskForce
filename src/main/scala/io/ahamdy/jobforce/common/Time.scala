package io.ahamdy.jobforce.common

import java.time.{Instant, ZoneId, ZonedDateTime}

import fs2._

trait Time {
  def now: Task[ZonedDateTime] = Task.delay(unsafeNow())
  def epoch: ZonedDateTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))
  def unsafeNow(): ZonedDateTime
}

object TimeImpl extends Time {

  override def unsafeNow(): ZonedDateTime =
    ZonedDateTime.now.withZoneSameInstant(ZoneId.of("UTC"))

}

