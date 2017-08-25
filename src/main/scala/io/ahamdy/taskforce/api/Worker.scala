package io.ahamdy.taskforce.api

import fs2.Task
import io.ahamdy.taskforce.domain.QueuedJob

trait Worker {
  def queueJob(queuedJob: QueuedJob): Task[Boolean]
}
