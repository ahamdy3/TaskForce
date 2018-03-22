package io.ahamdy.taskforce.api

import monix.eval.Task
import io.ahamdy.taskforce.domain.QueuedJob

trait Worker {
  def queueJob(queuedJob: QueuedJob): Task[Boolean]
}
