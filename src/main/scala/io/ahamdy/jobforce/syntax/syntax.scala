package io.ahamdy.jobforce

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

import scala.concurrent.duration._

package object syntax {

  implicit class ZonedDateTimeSyntax(zonedDateTime: ZonedDateTime) {
    def minus(zonedDateTimeB: ZonedDateTime): FiniteDuration =
      Duration.apply(ChronoUnit.NANOS.between(zonedDateTime, zonedDateTimeB), NANOSECONDS)

    def minus(duration: FiniteDuration): ZonedDateTime =
      zonedDateTime.minusSeconds(duration.toSeconds)

    def isBetween(olderTime: ZonedDateTime, newerTime: ZonedDateTime): Boolean =
      zonedDateTime.isAfter(olderTime) && zonedDateTime.isBefore(newerTime)
  }
}

