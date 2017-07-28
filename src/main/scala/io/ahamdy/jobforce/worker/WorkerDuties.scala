package io.ahamdy.jobforce.worker

import java.util.concurrent.ConcurrentHashMap

import cats.syntax.either._
import cats.syntax.flatMap._
import fs2.interop.cats._
import fs2.Task
import io.ahamdy.jobforce.common.{Logging, Time}
import io.ahamdy.jobforce.domain._
import io.ahamdy.jobforce.shared.NodeInfoProvider
import io.ahamdy.jobforce.store.{JobsStore, NodeStore}
import io.ahamdy.jobforce.syntax._

trait UserApi {
  def queueJob(queuedJob: QueuedJob): Task[Boolean]
}

trait WorkerDuties extends UserApi {
  def requeueJob(runningJob: RunningJob, resultMessage: Option[JobResultMessage] = None): Task[Unit]

  def finishJob(finishedJob: FinishedJob): Task[Unit]

  def runAssignedJobs: Task[Unit]

  def localRunningJobs: ConcurrentHashMap[JobId, RunningJob]

  def signalHeartbeat: Task[Unit]
}

class WorkerDutiesImpl(config: WorkerDutiesConfig, jobsStore: JobsStore, nodeInfoProvider: NodeInfoProvider,
                       nodeStore: NodeStore, jobHandlerRegister: JobHandlerRegister, time: Time)
  extends WorkerDuties with Logging {

  val localRunningJobs = new ConcurrentHashMap[JobId, RunningJob]()

  override def queueJob(queuedJob: QueuedJob): Task[Boolean] =
    jobsStore.createQueuedJob(queuedJob)

  override def requeueJob(runningJob: RunningJob, resultMessage: Option[JobResultMessage] = None): Task[Unit] =
    if (runningJob.attempts.attempts < runningJob.attempts.maxAttempts)
      for {
        now <- time.now
        _ <- queueJob(runningJob.toQueuedJob(now))
      } yield ()
    else
      for {
        now <- time.now
        _ <- finishJob(runningJob.toFinishedJob(now, JobResult.Failure, resultMessage))
      } yield ()

  override def finishJob(finishedJob: FinishedJob): Task[Unit] =
    jobsStore.moveRunningJobToFinishedJob(finishedJob)

  override def runAssignedJobs: Task[Unit] =
    jobsStore.getRunningJobsByNodeId(nodeInfoProvider.nodeId).flatMap { jobs =>
      parallelSequenceUnit(jobs.map(runAssignedJob))
    }

  def runAssignedJob(runningJob: RunningJob): Task[Unit] =
    jobHandlerRegister.getJobHandler(runningJob.jobType) match {
      case Some(handler) if !localRunningJobs.containsKey(runningJob.id) =>
        runAssignedJobWithHandler(handler, runningJob)
      case Some(_) => Task.now(())
      case None => logError(s"${runningJob.jobType} has no registered job handler!")
    }

  def runAssignedJobWithHandler(jobHandler: JobHandler, job: RunningJob): Task[Unit] = {
    for {
      validData <- runValidation(jobHandler, job)
      _ <- jobHandler.jobHandlerFunction(validData, this).attempt.map(_.leftMap(jobHandler.errorHandler)).attempt
      finishTime <- time.now
      _ <- finishJob(job.toFinishedJob(finishTime, JobResult.Success))
    } yield ()
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

  override def signalHeartbeat: Task[Unit] =
    nodeStore.updateHeartbeat(nodeInfoProvider.nodeGroup, nodeInfoProvider.nodeId)
}

case class WorkerDutiesConfig()
