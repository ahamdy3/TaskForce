package io.ahamdy.jobforce.worker

import fs2.Task
import io.ahamdy.jobforce.domain._

trait JobHandler {

  val jobType: JobType
  /**
    * validates data provided for the job
    *
    * @param data
    * @return Success Task of validated data or failed Task with JobDataValidationException
    */
  def validateJobInput(data: Map[String, String]): Task[Map[String, String]]

  def jobHandlerFunction(validData: Map[String, String], worker: WorkerApi): Task[Unit]

  def errorHandler: PartialFunction[Throwable, Task[Unit]]

}
