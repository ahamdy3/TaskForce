package io.ahamdy.taskforce.scheduling

import cats.effect.IO
import io.ahamdy.taskforce.domain.ScheduledJob

trait JobsScheduleProvider {
  def getJobsSchedule: IO[List[ScheduledJob]]
}
