package io.ahamdy.taskforce.worker

import java.util.concurrent.atomic.AtomicInteger

import fs2.Task
import io.ahamdy.taskforce.api.Worker
import io.ahamdy.taskforce.common.Logging
import io.ahamdy.taskforce.domain.{JobErrorDirective, JobErrorMessage, JobType}

class DummyJobHandler(val jobType: JobType) extends JobHandler with Logging {

  val totalRuns = new AtomicInteger()
  val successfulRuns = new AtomicInteger()
  val failedRuns = new AtomicInteger()

  override def validateJobInput(data: Map[String, String]): Task[Map[String, String]] = Task.now(data)

  override def jobHandlerFunction(validData: Map[String, String], worker: Worker): Task[Unit] = Task.delay{
    totalRuns.incrementAndGet()
    logger.info(s"running test job handler with data $validData")

    if(validData.keySet.contains("FAIL"))
      throw new Exception("Fake test exception")

    successfulRuns.incrementAndGet()
  }

  override def errorHandler: PartialFunction[Throwable, (JobErrorDirective, JobErrorMessage)] = {
    case t =>
      failedRuns.incrementAndGet()
      (JobErrorDirective.Retry, JobErrorMessage(t.getMessage))
  }
}
