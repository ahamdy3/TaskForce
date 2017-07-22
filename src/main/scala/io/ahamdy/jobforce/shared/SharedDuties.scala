package io.ahamdy.jobforce.shared

import fs2.Task
import io.ahamdy.jobforce.common.Logging
import io.ahamdy.jobforce.db.JobsStore
import io.ahamdy.jobforce.domain.QueuedJob

trait SharedDuties {
  def queueJob(queuedJobInstance: QueuedJob): Task[Unit]
}

class SharedDutiesImpl(jobsStore: JobsStore) extends SharedDuties with Logging {

  override def queueJob(queuedJobInstance: QueuedJob): Task[Unit] = {
    jobsStore.createQueuedJob(queuedJobInstance).flatMap {
      case true => logInfo(s"${queuedJobInstance.id} successfully queued")
      case false => logInfo(s"${queuedJobInstance.id} is already queued")
    }
  }
}
