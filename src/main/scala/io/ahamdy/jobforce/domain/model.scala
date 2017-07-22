package io.ahamdy.jobforce.domain

import java.time.ZonedDateTime

case class JobId(value: String) extends AnyVal
case class JobLock(value: String) extends AnyVal
case class JobType(value: String) extends AnyVal
case class JobWeight(value: Int) extends AnyVal
case class NodeId(value: String) extends AnyVal
case class NodeGroup(value: String) extends AnyVal
case class NodeActive(value: Boolean) extends AnyVal
case class JobAttempts(attempts: Int, maxAttempts: Int) {
  def incAttempts: JobAttempts = JobAttempts(attempts +1, maxAttempts)
}

abstract class JobInstance {
  def jobId: JobId
  def jobLock: JobLock
  def jobType: JobType
  def jobWeight: JobWeight
  def jobData: Map[String, String]
  def jobAttempts: JobAttempts
  def parentJob: Option[JobId]
}

case class QueuedJobInstance(jobId: JobId, jobLock: JobLock, jobType: JobType,
                             jobWeight: JobWeight, jobData: Map[String, String], jobAttempts: JobAttempts,
                             queuingTime: ZonedDateTime, parentJob: Option[JobId]) extends JobInstance {
  def toRunningJobInstance(nodeId: NodeId, startTime: ZonedDateTime): RunningJobInstance =
    RunningJobInstance(jobId, nodeId, jobLock, jobType, jobWeight, jobData, jobAttempts.incAttempts, queuingTime,
      startTime, parentJob)
}

case class RunningJobInstance(jobId: JobId, nodeId: NodeId, jobLock: JobLock, jobType: JobType,
                              jobWeight: JobWeight, jobData: Map[String, String], jobAttempts: JobAttempts,
                              queuingTime: ZonedDateTime, startTime: ZonedDateTime,
                              parentJob: Option[JobId]) extends JobInstance {
  def toQueuedJobInstance(newQueuingTime: ZonedDateTime): QueuedJobInstance =
    QueuedJobInstance(jobId, jobLock, jobType, jobWeight, jobData, jobAttempts, newQueuingTime, parentJob)
  def toFinishedJobInstance(finishTime: ZonedDateTime): FinishedJobInstance =
    FinishedJobInstance(jobId, nodeId, jobLock, jobType, jobWeight, jobData, jobAttempts.incAttempts, queuingTime,
      startTime, finishTime, parentJob)
}

case class FinishedJobInstance(jobId: JobId, nodeId: NodeId, jobLock: JobLock, jobType: JobType,
                               jobWeight: JobWeight, jobData: Map[String, String], jobAttempts: JobAttempts,
                               queuingTime: ZonedDateTime, startTime: ZonedDateTime, finishTime: ZonedDateTime,
                               parentJob: Option[JobId]) extends JobInstance

case class JobNode(nodeId: NodeId, nodeGroup: NodeGroup, startTime: ZonedDateTime, active: NodeActive)
case class NodeLoad(node: JobNode, jobsWeight: Int)