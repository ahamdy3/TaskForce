package io.ahamdy.jobforce

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

import scala.concurrent.duration._

package object syntax {

  implicit class ZonedDateTimeSyntax(zonedDateTime: ZonedDateTime) {
    def minus(zonedDateTimeB: ZonedDateTime): Duration =
      Duration.apply(ChronoUnit.NANOS.between(zonedDateTime, zonedDateTimeB), NANOSECONDS)
  }
}

