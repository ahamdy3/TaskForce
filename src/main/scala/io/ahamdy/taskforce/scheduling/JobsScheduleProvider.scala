package io.ahamdy.taskforce.scheduling

import monix.eval.Task
import io.ahamdy.taskforce.domain.ScheduledJob

trait JobsScheduleProvider {
  def getJobsSchedule: Task[List[ScheduledJob]]
}
