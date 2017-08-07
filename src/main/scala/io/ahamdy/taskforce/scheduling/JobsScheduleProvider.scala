package io.ahamdy.taskforce.scheduling

import fs2.Task
import io.ahamdy.taskforce.domain.ScheduledJob

trait JobsScheduleProvider {
  def getJobsSchedule: Task[List[ScheduledJob]]
}
