package io.ahamdy.jobforce.db

import java.time.ZonedDateTime

import fs2.Task
import io.ahamdy.jobforce.domain._


trait JobsStore {
  def getQueuedJobs: Task[List[QueuedJob]]
  def getRunningJobs: Task[List[RunningJob]]
  def getRunningJobsByNodeId(nodeId: NodeId): Task[List[RunningJob]]
  def getFinishedJobs: Task[List[FinishedJob]]

  def createQueuedJob(queuedJob: QueuedJob): Task[Boolean]
  // def createRunningJob(runningJob: RunningJobInstance): Task[Unit]
  // def createFinishedJob(finishedJob: FinishedJobInstance): Task[Unit]

  def moveQueuedJobToRunningJob(runningJob: RunningJob): Task[Unit]

  def moveRunningJobToQueuedJob(queuedJob: QueuedJob): Task[Unit]
  def moveRunningJobToFinishedJob(finishedJob: FinishedJob): Task[Unit]

  def getJobLastRunTime(lock: JobLock): Task[Option[ZonedDateTime]]
}
