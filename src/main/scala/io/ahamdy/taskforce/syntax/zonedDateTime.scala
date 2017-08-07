package io.ahamdy.taskforce.syntax

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

import scala.concurrent.duration._

trait ZonedDateTimeSyntax {

  implicit class ZonedDateTimeSyntax(val zonedDateTime: ZonedDateTime) {
    def minus(zonedDateTimeB: ZonedDateTime): FiniteDuration =
      Duration.apply(ChronoUnit.NANOS.between(zonedDateTimeB, zonedDateTime), NANOSECONDS)

    def durationBetween(zonedDateTimeB: ZonedDateTime): FiniteDuration =
      if(zonedDateTimeB.isBefore(zonedDateTime))
        minus(zonedDateTimeB)
      else
        zonedDateTimeB.minus(zonedDateTime)

    def minus(duration: FiniteDuration): ZonedDateTime =
      zonedDateTime.minusSeconds(duration.toSeconds)

    def plus(duration: FiniteDuration): ZonedDateTime =
      zonedDateTime.plusSeconds(duration.toSeconds)

    def isBetween(timeA: ZonedDateTime, timeB: ZonedDateTime): Boolean =
      ( (zonedDateTime.isAfter(timeA) || zonedDateTime.isEqual(timeA)) && (zonedDateTime.isBefore(timeB) || zonedDateTime.isEqual(timeA)) ) ||
        ( (zonedDateTime.isAfter(timeB) || zonedDateTime.isEqual(timeB)) && (zonedDateTime.isBefore(timeA) || zonedDateTime.isEqual(timeA)) )

    def isBetweenExclusive(timeA: ZonedDateTime, timeB: ZonedDateTime): Boolean =
      (zonedDateTime.isAfter(timeA)  && zonedDateTime.isBefore(timeB)) ||
        (zonedDateTime.isAfter(timeB) && zonedDateTime.isBefore(timeA))
  }
}

object zonedDateTime extends ZonedDateTimeSyntax

