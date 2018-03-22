package io.ahamdy.taskforce.scheduling

import monix.eval.Task
import io.ahamdy.taskforce.domain.ScheduledJob

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DummyJobsScheduleProvider extends JobsScheduleProvider{
  val scheduledJobs: ListBuffer[ScheduledJob] = mutable.ListBuffer.empty
  override def getJobsSchedule: Task[List[ScheduledJob]] =
    Task(scheduledJobs.toList)

  def reset(): Unit = scheduledJobs.clear()
}
