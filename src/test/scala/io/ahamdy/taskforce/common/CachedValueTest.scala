package io.ahamdy.taskforce.common

import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger

import cats.effect.IO
import io.ahamdy.taskforce.testing.StandardSpec

import scala.concurrent.duration._

class CachedValueTest extends StandardSpec {

  "CachedValue" should {
    "value should cache values during TTL" in {
      val now = ZonedDateTime.now()
      val dummyTime = new DummyTime(now)
      val accessCounter = new AtomicInteger()
      val source = IO(accessCounter.incrementAndGet())

      val cachedValue = new CachedValue(source, 1.minute, dummyTime)

      cachedValue.value must beSucceedingTask(1)
      dummyTime.currentTime.set(now.plusSeconds(10))
      cachedValue.value must beSucceedingTask(1)
      dummyTime.currentTime.set(now.plusMinutes(1).plusSeconds(1))
      cachedValue.value must beSucceedingTask(2)
      dummyTime.currentTime.set(now.plusMinutes(1).plusSeconds(10))
      cachedValue.value must beSucceedingTask(2)
      dummyTime.currentTime.set(now.plusMinutes(2).plusSeconds(10))
      cachedValue.value must beSucceedingTask(3)
    }

  }
}
