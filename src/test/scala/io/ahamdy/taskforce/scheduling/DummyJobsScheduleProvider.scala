package io.ahamdy.taskforce.scheduling

import cats.effect.IO
import io.ahamdy.taskforce.domain.ScheduledJob

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DummyJobsScheduleProvider extends JobsScheduleProvider{
  val scheduledJobs: ListBuffer[ScheduledJob] = mutable.ListBuffer.empty
  override def getJobsSchedule: IO[List[ScheduledJob]] =
    IO(scheduledJobs.toList)

  def reset(): Unit = scheduledJobs.clear()
}
