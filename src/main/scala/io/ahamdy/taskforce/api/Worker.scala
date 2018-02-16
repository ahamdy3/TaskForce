package io.ahamdy.taskforce.api

import cats.effect.IO
import io.ahamdy.taskforce.domain.QueuedJob

trait Worker {
  def queueJob(queuedJob: QueuedJob): IO[Boolean]
}
