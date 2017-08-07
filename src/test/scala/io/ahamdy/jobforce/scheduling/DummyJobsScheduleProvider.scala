package io.ahamdy.jobforce.scheduling

import fs2.Task
import io.ahamdy.jobforce.domain.ScheduledJob

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DummyJobsScheduleProvider extends JobsScheduleProvider{
  val scheduledJobs: ListBuffer[ScheduledJob] = mutable.ListBuffer.empty
  override def getJobsSchedule: Task[List[ScheduledJob]] =
    Task.delay(scheduledJobs.toList)

  def reset(): Unit = scheduledJobs.clear()
}
