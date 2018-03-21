package io.ahamdy.taskforce.worker

import java.util.concurrent.ConcurrentHashMap

import cats.effect.IO
import cats.syntax.either._
import cats.syntax.flatMap._
import io.ahamdy.taskforce.api.{NodeInfoProvider, Worker}
import io.ahamdy.taskforce.common.Logging
import io.ahamdy.taskforce.common.Time
import io.ahamdy.taskforce.domain._
import io.ahamdy.taskforce.store.{JobStore, NodeStore}
import io.ahamdy.taskforce.syntax.IOType._

trait WorkerDuties extends Worker {
  def localRunningJobs: ConcurrentHashMap[JobId, RunningJob]

  def runAssignedJobs: IO[Unit]

  /*  def requeueJob(runningJob: RunningJob, resultMessage: Option[JobResultMessage] = None): IO[Unit]
    def finishJob(finishedJob: FinishedJob): IO[Unit]*/

  // def signalHeartbeat: IO[Unit]
}

class WorkerDutiesImpl(config: WorkerDutiesConfig, jobStore: JobStore, nodeInfoProvider: NodeInfoProvider,
  nodeStore: NodeStore, jobHandlerRegister: JobHandlerRegister, time: Time)
  extends WorkerDuties with Logging {

  val localRunningJobs = new ConcurrentHashMap[JobId, RunningJob]()

  override def queueJob(queuedJob: QueuedJob): IO[Boolean] =
    jobStore.createQueuedJob(queuedJob)

  def requeueJob(runningJob: RunningJob, resultMessage: Option[JobResultMessage] = None): IO[Unit] =
    if (runningJob.attempts.attempts < runningJob.attempts.maxAttempts.value)
      for {
        now <- time.now
        _ <- jobStore.moveRunningJobToQueuedJob(runningJob.toQueuedJob(now))
      } yield ()
    else
      for {
        now <- time.now
        _ <- finishJob(runningJob.toFinishedJob(now, JobResult.Failure, resultMessage))
      } yield ()

  def finishJob(finishedJob: FinishedJob): IO[Unit] =
    jobStore.moveRunningJobToFinishedJob(finishedJob)

  override def runAssignedJobs: IO[Unit] =
    jobStore.getRunningJobsByNodeId(nodeInfoProvider.nodeId).flatMap { jobs =>
      ??? //parallelSequenceUnit(jobs.map(runAssignedJob))(jobsStrategy)
    }

  def runAssignedJob(runningJob: RunningJob): IO[Unit] =
    ifNotAlreadyRunning(runningJob.id) {
      jobHandlerRegister.getJobHandler(runningJob.jobType) match {
        case Some(handler) =>
          runAssignedJobWithHandler(handler, runningJob).flatMap {
            case Right(_) => time.now.flatMap(now => finishJob(runningJob.toFinishedJob(now, JobResult.Success)))
            case Left((directive, errorMsg)) if directive == JobErrorDirective.Retry =>
              requeueJob(runningJob, Some(JobResultMessage(errorMsg.value)))
            case Left((directive, errorMsg)) if directive == JobErrorDirective.Abort =>
              time.now.flatMap(now => finishJob(runningJob.toFinishedJob(now, JobResult.Failure, Some(JobResultMessage(errorMsg.value)))))

          }
        case None => logError(s"${runningJob.jobType} has no registered job handler!")
      }
    }

  def runAssignedJobWithHandler(jobHandler: JobHandler, job: RunningJob): IO[Either[(JobErrorDirective, JobErrorMessage), Unit]] = {
    for {
      validData <- runValidation(jobHandler, job)
      result <- jobHandler.jobHandlerFunction(validData, this).attempt.map(_.leftMap(jobHandler.errorHandler))
    } yield result
  }

  def runValidation(jobHandler: JobHandler, job: RunningJob): IO[Map[String, String]] = {
    jobHandler.validateJobInput(job.data).attempt flatMap {
      case Right(validData) => IO.pure(validData)
      case Left(e: JobDataValidationException) =>
        time.now.flatMap { now =>
          finishJob(job.toFinishedJob(now, JobResult.Failure,
            Some(JobResultMessage(s"Job data validation error: ${e.msg}")))) >>
            IO.raiseError(e)
        }
      case Left(t) => IO.raiseError(t)
    }
  }

  def ifNotAlreadyRunning(jobId: JobId)(jobTask: IO[Unit]): IO[Unit] =
    if (!localRunningJobs.containsKey(jobId))
      jobTask
    else
      IO.unit

  /*override def signalHeartbeat: IO[Unit] =
    nodeStore.updateHeartbeat(nodeInfoProvider.nodeGroup, nodeInfoProvider.nodeId)*/
}

case class WorkerDutiesConfig()
