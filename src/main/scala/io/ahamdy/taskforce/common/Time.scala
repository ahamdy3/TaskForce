package io.ahamdy.taskforce.common

import java.time.{Instant, ZoneId, ZonedDateTime}

import monix.eval.Task


trait Time {
  def now: Task[ZonedDateTime] = Task(unsafeNow())
  def epoch: ZonedDateTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))
  def unsafeNow(): ZonedDateTime
}

object TimeImpl extends Time {

  override def unsafeNow(): ZonedDateTime =
    ZonedDateTime.now.withZoneSameInstant(ZoneId.of("UTC"))

}

