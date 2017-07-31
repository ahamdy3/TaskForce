package io.ahamdy.jobforce.scheduling

import java.time.{ZoneId, ZonedDateTime}
import cats.implicits._
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.model.{Cron, CronType}
import com.cronutils.parser.CronParser


case class CronLine(cron: Cron, timeZone: ZoneId) {

  /** Parses the cron spec and checks if it's a match to the given point in time */
  def matches(time: ZonedDateTime): Boolean =
    ExecutionTime.forCron(cron).isMatch(time.withZoneSameInstant(timeZone))

  def latestExecutionTimeBefore(time: ZonedDateTime): ZonedDateTime =
    ExecutionTime.forCron(cron).lastExecution(time.withZoneSameInstant(timeZone))

  def nextExecutionTimeAfter(time: ZonedDateTime): ZonedDateTime =
    ExecutionTime.forCron(cron).nextExecution(time.withZoneSameInstant(timeZone))

  override def toString: String = s"CronLine(${cron.asString}, $timeZone)"
}

object CronLine {

  def parse(cron: String, syntax: CronType, timeZone: ZoneId): Option[CronLine] = {
    val parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(syntax))
    Either.catchOnly[IllegalArgumentException](
      try {
        parser.parse(cron)

      } catch {
        case e: Throwable => println(e)
          throw e
      }).toOption.map(cron => CronLine(cron, timeZone))
  }
}
