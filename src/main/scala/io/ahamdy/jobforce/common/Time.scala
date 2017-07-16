package io.ahamdy.jobforce.common

import java.time.{ZoneId, ZonedDateTime}

import fs2._

trait Time {
  def now: Task[ZonedDateTime] = Task.delay(unsafeNow())
  def unsafeNow(): ZonedDateTime
}

object TimeImpl extends Time {

  def unsafeNow(): ZonedDateTime =
    ZonedDateTime.now.withZoneSameInstant(ZoneId.of("UTC"))
}

