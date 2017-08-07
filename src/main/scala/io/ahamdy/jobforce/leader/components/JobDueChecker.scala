package io.ahamdy.jobforce.leader.components

import java.time.ZonedDateTime

import io.ahamdy.jobforce.domain.JobSchedule
import io.ahamdy.jobforce.syntax.zonedDateTime._

object JobDueChecker {
  def isDue(schedule: JobSchedule, now: ZonedDateTime, actualLastTimeRun: ZonedDateTime): Boolean = {
    val scheduledLastTime = schedule.cronLine.latestExecutionTimeBefore(now)
    val nextExecutionTime = schedule.cronLine.nextExecutionTimeAfter(scheduledLastTime)

    (actualLastTimeRun.plus(schedule.startTimeWindow).isBefore(scheduledLastTime) &&
      schedule.cronLine.toDurationOn(now) > actualLastTimeRun.durationBetween(now)) ||
      (scheduledLastTime.isBetween(now, now.minus(schedule.startTimeWindow)) ||
        nextExecutionTime.isBetween(now, now.minus(schedule.startTimeWindow)))
  }
}
