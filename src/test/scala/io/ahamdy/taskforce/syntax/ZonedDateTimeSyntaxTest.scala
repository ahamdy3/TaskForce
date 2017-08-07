package io.ahamdy.taskforce.syntax

import java.time.{ZoneId, ZonedDateTime}

import io.ahamdy.taskforce.syntax.zonedDateTime._
import io.ahamdy.taskforce.testing.StandardSpec

import scala.concurrent.duration._

class ZonedDateTimeSyntaxTest extends StandardSpec {

  val currentTime: ZonedDateTime = ZonedDateTime.now()
  val fiveMinLater: ZonedDateTime = currentTime.plusMinutes(5)
  val tenMinLater: ZonedDateTime = currentTime.plusMinutes(10)

  "ZonedDateTimeSyntax" should {
    "minus should subtract given ZonedDateTime from current one and return FiniteDuration" in {
      tenMinLater.minus(currentTime) mustEqual 10.minutes
    }

    "durationBetween should return FiniteDuration between given ZonedDateTime and current ZonedDateTime" in {
      tenMinLater.durationBetween(currentTime) mustEqual 10.minutes
      currentTime.durationBetween(tenMinLater) mustEqual 10.minutes
    }

    "minus should subtract given FiniteDuration from current one and return ZonedDateTime" in {
      tenMinLater.minus(10.minutes) mustEqual currentTime
    }

    "plus should add giving FiniteDuration from current one and return ZonedDateTime" in {
      currentTime.plus(10.minutes) mustEqual tenMinLater
    }

    "isBetween should return true if current ZonedDataTime is between two given ZonedDataTime" in {
      fiveMinLater.isBetween(currentTime, tenMinLater) must beTrue
      fiveMinLater.isBetween(tenMinLater, currentTime) must beTrue

      tenMinLater.isBetween(currentTime, fiveMinLater) must beFalse
      tenMinLater.isBetween(fiveMinLater, currentTime) must beFalse
    }

    "isBetween should be inclusive" in {
      fiveMinLater.isBetween(fiveMinLater, tenMinLater) must beTrue
      fiveMinLater.isBetween(fiveMinLater, currentTime) must beTrue
    }

    "isBetween should return true if current ZonedDataTime is between two given ZonedDataTime with different timezones" in {
      fiveMinLater.isBetween(currentTime, tenMinLater) must beTrue
      fiveMinLater.isBetween(currentTime.withZoneSameInstant(ZoneId.of("Europe/Berlin")),
        tenMinLater.withZoneSameInstant(ZoneId.of("UTC"))) must beTrue


      tenMinLater.isBetween(currentTime, fiveMinLater) must beFalse
      tenMinLater.isBetween(currentTime.withZoneSameInstant(ZoneId.of("Europe/Berlin")),
        fiveMinLater.withZoneSameInstant(ZoneId.of("UTC"))) must beFalse
    }

    "isBetweenExclusive should return true if current ZonedDataTime is between two given ZonedDataTime" in {
      fiveMinLater.isBetweenExclusive(currentTime, tenMinLater) must beTrue
      fiveMinLater.isBetweenExclusive(tenMinLater, currentTime) must beTrue

      tenMinLater.isBetweenExclusive(currentTime, fiveMinLater) must beFalse
      tenMinLater.isBetweenExclusive(fiveMinLater, currentTime) must beFalse
    }

    "isBetweenExclusive should be exclusive" in {
      fiveMinLater.isBetweenExclusive(fiveMinLater, tenMinLater) must beFalse
      fiveMinLater.isBetweenExclusive(fiveMinLater, currentTime) must beFalse
    }
  }
}
