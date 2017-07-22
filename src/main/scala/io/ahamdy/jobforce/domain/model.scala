package io.ahamdy.jobforce.domain

import java.time.ZonedDateTime
import java.util.UUID

import io.ahamdy.jobforce.scheduling.CronLine

import scala.concurrent.duration.FiniteDuration

case class NodeId(value: String) extends AnyVal
case class NodeGroup(value: String) extends AnyVal
case class NodeActive(value: Boolean) extends AnyVal
case class NodeVersion(value: String) extends AnyVal

case class JobId(value: String) extends AnyVal
case class JobLock(value: String) extends AnyVal
case class JobType(value: String) extends AnyVal
case class JobWeight(value: Int) extends AnyVal
case class JobPriority(value: Int) extends AnyVal
case class JobSchedule(cronLine: CronLine, startTimeWindow: FiniteDuration)

case class JobAttempts(attempts: Int, maxAttempts: Int) {
  def incAttempts: JobAttempts = JobAttempts(attempts +1, maxAttempts)
}

object JobId {
  def generateNew: JobId = JobId(UUID.randomUUID().toString)
}

object JobAttempts {
  def withMaxAttempts(maxAttempts: Int = 5): JobAttempts =
    JobAttempts(attempts = 0, maxAttempts = maxAttempts)
}

abstract class JobInstance {
  def id: JobId
  def lock: JobLock
  def jobType: JobType
  def weight: JobWeight
  def data: Map[String, String]
  def attempts: JobAttempts
  def priority: JobPriority
  def parentJob: Option[JobId]
}

case class ScheduledJob(lock: JobLock, jobType: JobType, weight: JobWeight, data: Map[String, String],
                        schedule: JobSchedule, attempts: JobAttempts, priority: JobPriority) {
  def toQueuedJob(id: JobId, queuingTime: ZonedDateTime): QueuedJob =
    QueuedJob(id, lock, jobType, weight, data, attempts, priority, queuingTime, None)
}

case class QueuedJob(id: JobId, lock: JobLock, jobType: JobType, weight: JobWeight, data: Map[String, String],
                     attempts: JobAttempts, priority: JobPriority, queuingTime: ZonedDateTime,
                     parentJob: Option[JobId]) extends JobInstance {
  def toRunningJob(nodeId: NodeId, startTime: ZonedDateTime): RunningJob =
    RunningJob(id, nodeId, lock, jobType, weight, data, attempts.incAttempts, priority, queuingTime,
      startTime, parentJob)
}

case class RunningJob(id: JobId, nodeId: NodeId, lock: JobLock, jobType: JobType, weight: JobWeight,
                      data: Map[String, String], attempts: JobAttempts, priority: JobPriority,
                      queuingTime: ZonedDateTime, startTime: ZonedDateTime,
                      parentJob: Option[JobId]) extends JobInstance {
  def toQueuedJob(newQueuingTime: ZonedDateTime): QueuedJob =
    QueuedJob(id, lock, jobType, weight, data, attempts, priority, newQueuingTime, parentJob)
  def toFinishedJob(finishTime: ZonedDateTime): FinishedJob =
    FinishedJob(id, nodeId, lock, jobType, weight, data, attempts, priority, queuingTime,
      startTime, finishTime, parentJob)
}

case class FinishedJob(id: JobId, nodeId: NodeId, lock: JobLock, jobType: JobType,
                       weight: JobWeight, data: Map[String, String], attempts: JobAttempts, priority: JobPriority,
                       queuingTime: ZonedDateTime, startTime: ZonedDateTime, finishTime: ZonedDateTime,
                       parentJob: Option[JobId]) extends JobInstance

case class JobNode(nodeId: NodeId, nodeGroup: NodeGroup, startTime: ZonedDateTime, active: NodeActive, version: NodeVersion)
case class NodeLoad(node: JobNode, jobsWeight: Int)