package io.ahamdy.jobforce.syntax

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

import scala.concurrent.duration._

trait ZonedDateTimeSyntax {

  implicit class ZonedDateTimeSyntax(val zonedDateTime: ZonedDateTime) {
    def minus(zonedDateTimeB: ZonedDateTime): FiniteDuration =
      Duration.apply(ChronoUnit.NANOS.between(zonedDateTimeB, zonedDateTime), NANOSECONDS)

    def minus(duration: FiniteDuration): ZonedDateTime =
      zonedDateTime.minusSeconds(duration.toSeconds)

    def isBetween(timeA: ZonedDateTime, timeB: ZonedDateTime): Boolean =
      (zonedDateTime.isAfter(timeA) && zonedDateTime.isBefore(timeB)) ||
        (zonedDateTime.isAfter(timeB) && zonedDateTime.isBefore(timeA))
  }
}

object zonedDateTime extends ZonedDateTimeSyntax

