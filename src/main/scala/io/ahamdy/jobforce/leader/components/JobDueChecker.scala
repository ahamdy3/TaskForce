package io.ahamdy.jobforce.leader.components

import java.time.ZonedDateTime

import io.ahamdy.jobforce.domain.JobSchedule
import io.ahamdy.jobforce.syntax.zonedDateTime._

object JobDueChecker {
  def isDue(schedule: JobSchedule, now: ZonedDateTime, lastRunTime: ZonedDateTime): Boolean =
    schedule.cronLine.nextExecutionTimeAfter(lastRunTime)
      .isBetween(now.minus(schedule.startTimeWindow), now)
}
