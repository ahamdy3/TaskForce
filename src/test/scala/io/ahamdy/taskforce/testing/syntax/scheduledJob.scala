package io.ahamdy.taskforce.testing.syntax

import java.time.ZonedDateTime

import io.ahamdy.taskforce.common.Time
import io.ahamdy.taskforce.domain._

trait ScheduledJobSyntax {

  implicit class ScheduledJobWrapper(sjob: ScheduledJob) {
    def toQueuedJob(time: Time): QueuedJob = sjob.toQueuedJob(time.unsafeNow())

    def toRunningJob(nodeId: NodeId, nodeGroup: NodeGroup, queuingTime: ZonedDateTime = ZonedDateTime.now(),
      startTime: ZonedDateTime = ZonedDateTime.now()): RunningJob =
      sjob.toQueuedJob(queuingTime).toRunningJobAndIncAttempts(nodeId, nodeGroup, startTime)

    def toRunningJob(nodeId: NodeId, nodeGroup: NodeGroup, time: Time): RunningJob =
      sjob.toQueuedJob(time.unsafeNow()).toRunningJobAndIncAttempts(nodeId, nodeGroup, time.unsafeNow())

    def toFinishedJob(nodeId: NodeId, nodeGroup: NodeGroup, queuingTime: ZonedDateTime = ZonedDateTime.now(),
      startTime: ZonedDateTime = ZonedDateTime.now(), finishTime: ZonedDateTime = ZonedDateTime.now(),
      jobResult: JobResult): FinishedJob =
      sjob.toRunningJob(nodeId, nodeGroup, queuingTime, startTime).toFinishedJob(finishTime, jobResult)

    def toFinishedJob(nodeId: NodeId, nodeGroup: NodeGroup, time: Time, jobResult: JobResult): FinishedJob =
      sjob.toRunningJob(nodeId, nodeGroup, time.unsafeNow(), time.unsafeNow()).toFinishedJob(time.unsafeNow(), jobResult)
  }

}

object scheduledJob extends ScheduledJobSyntax