package io.ahamdy.jobforce.syntax

import java.time.ZonedDateTime

import io.ahamdy.jobforce.common.DummyTime
import io.ahamdy.jobforce.testing.StandardSpec
import io.ahamdy.jobforce.syntax.zonedDateTime._
import scala.concurrent.duration._

class ZonedDateTimeSyntaxTest extends StandardSpec {

  val dummyTime = new DummyTime(ZonedDateTime.now())

  "ZonedDateTimeSyntax" should {
    "minus should subtract giving ZonedDateTime from current one and return FiniteDuration" in {
      val currentTime = dummyTime.unsafeNow()
      val tenMinLater = currentTime.plusMinutes(10)

      tenMinLater.minus(currentTime) mustEqual 10.minutes
    }

    "minus should subtract giving FiniteDuration from current one and return ZonedDateTime" in {
      val currentTime = dummyTime.unsafeNow()
      val tenMinLater = currentTime.plusMinutes(10)

      tenMinLater.minus(10.minutes) mustEqual currentTime
    }

    "isBetween should return true if current ZonedDataTime is between two given ZonedDataTime" in {
      val currentTime = dummyTime.unsafeNow()
      val fiveMinLater = currentTime.plusMinutes(5)
      val tenMinLater = currentTime.plusMinutes(10)

      fiveMinLater.isBetween(currentTime, tenMinLater) must beTrue
      fiveMinLater.isBetween(tenMinLater, currentTime) must beTrue

      tenMinLater.isBetween(currentTime, fiveMinLater) must beFalse
      tenMinLater.isBetween(fiveMinLater, currentTime) must beFalse
    }
  }
}
