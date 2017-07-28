package io.ahamdy.jobforce.domain

import java.time.ZonedDateTime
import java.util.UUID

import enumeratum.{Enum, EnumEntry}
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
case class JobResultMessage(value: String) extends AnyVal
case class JobVersionRule(directive: VersionRuleDirective, nodeVersion: NodeVersion)

sealed trait JobResult extends EnumEntry with EnumEntry.Lowercase
object JobResult extends Enum[JobResult] {
  case object Success extends JobResult
  case object Failure extends JobResult

  val values = findValues
}

sealed trait VersionRuleDirective extends EnumEntry with EnumEntry.Lowercase
object VersionRuleDirective extends Enum[VersionRuleDirective] {
  case object AtLeast extends VersionRuleDirective
  case object AtMost extends VersionRuleDirective
  case object Exactly extends VersionRuleDirective
  case object AnyVersion extends VersionRuleDirective

  val values = findValues
}

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
  def versionRule: JobVersionRule
}

case class ScheduledJob(lock: JobLock, jobType: JobType, weight: JobWeight, data: Map[String, String],
                        schedule: JobSchedule, attempts: JobAttempts, priority: JobPriority,
                        versionRule: JobVersionRule = JobVersionRule(VersionRuleDirective.AnyVersion, NodeVersion("IGNORED"))) {
  def toQueuedJob(id: JobId, queuingTime: ZonedDateTime): QueuedJob =
    QueuedJob(id, lock, jobType, weight, data, attempts, priority, queuingTime, None, versionRule)
}

case class QueuedJob(id: JobId, lock: JobLock, jobType: JobType, weight: JobWeight, data: Map[String, String],
                     attempts: JobAttempts, priority: JobPriority, queuingTime: ZonedDateTime,
                     parentJob: Option[JobId], versionRule: JobVersionRule) extends JobInstance {
  def toRunningJobAndIncAttempts(nodeId: NodeId, startTime: ZonedDateTime): RunningJob =
    RunningJob(id, nodeId, lock, jobType, weight, data, attempts.incAttempts, priority, queuingTime,
      startTime, parentJob, versionRule)
}

case class RunningJob(id: JobId, nodeId: NodeId, lock: JobLock, jobType: JobType, weight: JobWeight,
                      data: Map[String, String], attempts: JobAttempts, priority: JobPriority,
                      queuingTime: ZonedDateTime, startTime: ZonedDateTime,
                      parentJob: Option[JobId], versionRule: JobVersionRule) extends JobInstance {
  def toQueuedJob(newQueuingTime: ZonedDateTime): QueuedJob =
    QueuedJob(id, lock, jobType, weight, data, attempts, priority, newQueuingTime, parentJob, versionRule)
  def toFinishedJob(finishTime: ZonedDateTime, result: JobResult, resultMessage: Option[JobResultMessage] = None): FinishedJob =
    FinishedJob(id, nodeId, lock, jobType, weight, data, attempts, priority, queuingTime,
      startTime, finishTime, parentJob, result, resultMessage, versionRule)
}

case class FinishedJob(id: JobId, nodeId: NodeId, lock: JobLock, jobType: JobType,
                       weight: JobWeight, data: Map[String, String], attempts: JobAttempts, priority: JobPriority,
                       queuingTime: ZonedDateTime, startTime: ZonedDateTime, finishTime: ZonedDateTime,
                       parentJob: Option[JobId], result: JobResult, resultMessage: Option[JobResultMessage],
                       versionRule: JobVersionRule) extends JobInstance

case class JobNode(nodeId: NodeId, nodeGroup: NodeGroup, startTime: ZonedDateTime, active: NodeActive, version: NodeVersion)
case class NodeLoad(node: JobNode, jobsWeight: Int)

case class JobDataValidationException(msg: String) extends Exception(msg)
case class RegisterError(jobType: JobType, message: String)