package io.ahamdy.jobforce

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

import fs2.{Strategy, Task}
import cats.syntax.functor._
import fs2.interop.cats._

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

  def sequenceUnit[A](input: List[Task[A]]): Task[Unit] =
    Task.traverse(input)(identity).map(_.toList).as(())

  def parallelSequenceUnit[A](input: List[Task[A]])(implicit S: Strategy): Task[Unit] =
    Task.parallelTraverse(input)(identity).map(_.toList).as(())
}

