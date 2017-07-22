package io.ahamdy.jobforce.shared

import fs2.Task
import io.ahamdy.jobforce.common.Logging
import io.ahamdy.jobforce.db.JobsStore
import io.ahamdy.jobforce.domain.QueuedJobInstance

trait SharedDuties {
  def queueJob(queuedJobInstance: QueuedJobInstance): Task[Unit]
}

class SharedDutiesImpl(jobsStore: JobsStore) extends SharedDuties with Logging {

  override def queueJob(queuedJobInstance: QueuedJobInstance): Task[Unit] = {
    jobsStore.createQueuedJob(queuedJobInstance).flatMap {
      case true => logInfo(s"${queuedJobInstance.jobId} successfully queued")
      case false => logInfo(s"${queuedJobInstance.jobId} is already queued")
    }
  }
}
