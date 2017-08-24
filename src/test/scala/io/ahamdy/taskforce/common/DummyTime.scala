package io.ahamdy.taskforce.common

import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicReference

import io.ahamdy.taskforce.syntax.zonedDateTime._

import scala.concurrent.duration._


class DummyTime(now: ZonedDateTime) extends Time {
  val currentTime = new AtomicReference(now)
  override def unsafeNow(): ZonedDateTime = currentTime.get()

  def increaseBy(duration: FiniteDuration): Unit = currentTime.set(currentTime.get().plus(duration))
  def decreaseBy(duration: FiniteDuration): Unit = currentTime.set(currentTime.get().minus(duration))
}
