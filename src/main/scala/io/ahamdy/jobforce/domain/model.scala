package io.ahamdy.jobforce.domain

import java.time.ZonedDateTime


case class JobId(value: String) extends AnyVal
case class JobLock(value: String) extends AnyVal
case class JobType(value: String) extends AnyVal
case class JobWeight(value: Int) extends AnyVal
case class NodeId(value: String) extends AnyVal
case class JobAttempts(attempts: Int, maxAttempts: Int)

abstract class JobInstance {
  def jobId: JobId
  def nodeId: NodeId
  def jobLock: JobLock
  def jobType: JobType
  def jobWeight: JobWeight
  def jobData: Map[String,String]
  def jobAttempts: JobAttempts
}

case class QueuedJobInstance(jobId: JobId, nodeId: NodeId, jobLock: JobLock, jobType: JobType,
                             jobWeight: JobWeight, jobData: Map[String, String], jobAttempts: JobAttempts,
                             queuingTime: ZonedDateTime) extends JobInstance

case class RunningJobInstance(jobId: JobId, nodeId: NodeId, jobLock: JobLock, jobType: JobType,
                              jobWeight: JobWeight, jobData: Map[String, String], jobAttempts: JobAttempts,
                              queuingTime: ZonedDateTime, startTime: ZonedDateTime) extends JobInstance

case class FinishedJobInstance(jobId: JobId, nodeId: NodeId, jobLock: JobLock, jobType: JobType,
                              jobWeight: JobWeight, jobData: Map[String, String], jobAttempts: JobAttempts,
                              queuingTime: ZonedDateTime, startTime: ZonedDateTime, finishTime: ZonedDateTime) extends JobInstance

case class NodeLoad(nodeId: NodeId, jobsCount: Int)