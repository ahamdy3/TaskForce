package io.ahamdy.jobforce.db

import fs2.Task
import io.ahamdy.jobforce.domain.{FinishedJobInstance, QueuedJobInstance, RunningJobInstance}


trait JobsStore {
  def getQueuedJobs: Task[List[QueuedJobInstance]]
  def getRunningJobs: Task[List[RunningJobInstance]]
  def getFinishedJobs: Task[List[FinishedJobInstance]]

  def queueJob(queuedJob: QueuedJobInstance): Task[Boolean]
  def runJob(runningJob: RunningJobInstance): Task[Boolean]
  def finishJob(finishedJobInstance: FinishedJobInstance): Task[Unit]
}
