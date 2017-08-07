package io.ahamdy.taskforce.scheduling

import fs2.Task
import io.ahamdy.taskforce.domain.ScheduledJob

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DummyJobsScheduleProvider extends JobsScheduleProvider{
  val scheduledJobs: ListBuffer[ScheduledJob] = mutable.ListBuffer.empty
  override def getJobsSchedule: Task[List[ScheduledJob]] =
    Task.delay(scheduledJobs.toList)

  def reset(): Unit = scheduledJobs.clear()
}
