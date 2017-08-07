package io.ahamdy.jobforce.common

import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicReference


class DummyTime(now: ZonedDateTime) extends Time {
  val currentTime = new AtomicReference(now)
  override def unsafeNow(): ZonedDateTime = currentTime.get()
}
