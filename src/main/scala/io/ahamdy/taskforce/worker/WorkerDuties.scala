package io.ahamdy.taskforce.worker

import java.util.concurrent.ConcurrentHashMap

import cats.syntax.either._
import cats.syntax.flatMap._
import fs2.interop.cats._
import fs2.{Strategy, Task}
import io.ahamdy.taskforce.api.{NodeInfoProvider, Worker}
import io.ahamdy.taskforce.common.Logging
import io.ahamdy.taskforce.common.Time
import io.ahamdy.taskforce.domain._
import io.ahamdy.taskforce.store.{JobStore, NodeStore}
import io.ahamdy.taskforce.syntax.task._

trait WorkerDuties extends Worker {
  def localRunningJobs: ConcurrentHashMap[JobId, RunningJob]

  def runAssignedJobs: Task[Unit]

  /*  def requeueJob(runningJob: RunningJob, resultMessage: Option[JobResultMessage] = None): Task[Unit]
    def finishJob(finishedJob: FinishedJob): Task[Unit]*/

  // def signalHeartbeat: Task[Unit]
}

class WorkerDutiesImpl(config: WorkerDutiesConfig, jobStore: JobStore, nodeInfoProvider: NodeInfoProvider,
  nodeStore: NodeStore, jobHandlerRegister: JobHandlerRegister, time: Time, jobsStrategy: Strategy)
  extends WorkerDuties with Logging {

  val localRunningJobs = new ConcurrentHashMap[JobId, RunningJob]()

  override def queueJob(queuedJob: QueuedJob): Task[Boolean] =
    jobStore.createQueuedJob(queuedJob)

  def requeueJob(runningJob: RunningJob, resultMessage: Option[JobResultMessage] = None): Task[Unit] =
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

  def finishJob(finishedJob: FinishedJob): Task[Unit] =
    jobStore.moveRunningJobToFinishedJob(finishedJob)

  override def runAssignedJobs: Task[Unit] =
    jobStore.getRunningJobsByNodeId(nodeInfoProvider.nodeId).flatMap { jobs =>
      parallelSequenceUnit(jobs.map(runAssignedJob))(jobsStrategy)
    }

  def runAssignedJob(runningJob: RunningJob): Task[Unit] =
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

  def runAssignedJobWithHandler(jobHandler: JobHandler, job: RunningJob): Task[Either[(JobErrorDirective, JobErrorMessage), Unit]] = {
    for {
      validData <- runValidation(jobHandler, job)
      result <- jobHandler.jobHandlerFunction(validData, this).attempt.map(_.leftMap(jobHandler.errorHandler))
    } yield result
  }

  def runValidation(jobHandler: JobHandler, job: RunningJob): Task[Map[String, String]] = {
    jobHandler.validateJobInput(job.data).attempt flatMap {
      case Right(validData) => Task.now(validData)
      case Left(e: JobDataValidationException) =>
        time.now.flatMap { now =>
          finishJob(job.toFinishedJob(now, JobResult.Failure,
            Some(JobResultMessage(s"Job data validation error: ${e.msg}")))) >>
            Task.fail(e)
        }
      case Left(t) => Task.fail(t)
    }
  }

  def ifNotAlreadyRunning(jobId: JobId)(jobTask: Task[Unit]): Task[Unit] =
    if (!localRunningJobs.containsKey(jobId))
      jobTask
    else
      Task.unit

  /*override def signalHeartbeat: Task[Unit] =
    nodeStore.updateHeartbeat(nodeInfoProvider.nodeGroup, nodeInfoProvider.nodeId)*/
}

case class WorkerDutiesConfig()
