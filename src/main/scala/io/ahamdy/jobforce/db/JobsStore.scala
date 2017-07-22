package io.ahamdy.jobforce.db

import fs2.Task
import io.ahamdy.jobforce.domain.{FinishedJobInstance, QueuedJobInstance, RunningJobInstance}


trait JobsStore {
  def getQueuedJobs: Task[List[QueuedJobInstance]]
  def getRunningJobs: Task[List[RunningJobInstance]]
  def getFinishedJobs: Task[List[FinishedJobInstance]]

  def createQueuedJob(queuedJob: QueuedJobInstance): Task[Boolean]
  // def createRunningJob(runningJob: RunningJobInstance): Task[Unit]
  // def createFinishedJob(finishedJob: FinishedJobInstance): Task[Unit]

  def moveQueuedJobToRunningJob(runningJob: RunningJobInstance): Task[Unit]

  def moveRunningJobToQueuedJob(queuedJob: QueuedJobInstance): Task[Unit]
  def moveRunningJobToFinishedJob(finishedJob: FinishedJobInstance): Task[Unit]

}
