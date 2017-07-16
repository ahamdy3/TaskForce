package io.ahamdy.jobforce.scheduling

import fs2.Task
import io.ahamdy.jobforce.domain.ScheduledJob

trait JobsScheduleProvider {
  def getJobsSchedule: Task[List[ScheduledJob]]
}
