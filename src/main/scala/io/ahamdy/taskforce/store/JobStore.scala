package io.ahamdy.taskforce.store

import java.time.ZonedDateTime

import cats.effect.IO
import io.ahamdy.taskforce.domain._


trait JobStore {
  def getQueuedJobsOrderedByPriorityAndTime: IO[List[QueuedJob]]
  def getRunningJobs: IO[List[RunningJob]]
  def getRunningJobsByGroupName(nodeGroup: NodeGroup): IO[List[RunningJob]]
  def getRunningJobsByNodeId(nodeId: NodeId): IO[List[RunningJob]]
  def getFinishedJobs: IO[List[FinishedJob]]

  def createQueuedJob(queuedJob: QueuedJob): IO[Boolean]
  // def createRunningJob(runningJob: RunningJobInstance): IO[Unit]
  // def createFinishedJob(finishedJob: FinishedJobInstance): IO[Unit]

  def moveQueuedJobToRunningJob(runningJob: RunningJob): IO[Unit]

  def moveRunningJobToQueuedJob(queuedJob: QueuedJob): IO[Unit]
  def moveRunningJobToFinishedJob(finishedJob: FinishedJob): IO[Unit]

  def getJobLastRunTime(id: JobId): IO[Option[ZonedDateTime]]
}
