package io.ahamdy.taskforce.worker

import cats.effect.IO
import io.ahamdy.taskforce.api.Worker
import io.ahamdy.taskforce.domain._

trait JobHandler {

  val jobType: JobType
  /**
    * validates data provided for the job
    *
    * @param data
    * @return Success Task of validated data or failed Task with JobDataValidationException
    */
  def validateJobInput(data: Map[String, String]): IO[Map[String, String]]

  def jobHandlerFunction(validData: Map[String, String], worker: Worker): IO[Unit]

  def errorHandler: PartialFunction[Throwable, (JobErrorDirective, JobErrorMessage)]

}
