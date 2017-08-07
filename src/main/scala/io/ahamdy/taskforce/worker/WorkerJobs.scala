package io.ahamdy.taskforce.worker

import io.ahamdy.taskforce.common.Logging
import io.ahamdy.taskforce.scheduling.{Scheduler, SchedulerConfig, SchedulerImpl}

import scala.concurrent.duration.FiniteDuration

trait WorkerJobs {
  def scheduler: Scheduler
  def start(): Unit
  def shutdown(): Unit
}

class WorkerJobsImpl(config: WorkerJobsConfig, workerDuties: WorkerDuties) extends WorkerJobs with Logging {

  val cores = Runtime.getRuntime.availableProcessors()
  override val scheduler: Scheduler = new SchedulerImpl(SchedulerConfig(threadPoolSize = cores * config.threadPerCore))

  override def start(): Unit = {
    scheduler.unsafeSchedule(config.runJobsPeriod, workerDuties.runAssignedJobs, resultHandler)

    if(config.nodeStoreHeartbeat)
      scheduler.unsafeSchedule(config.heartBeatPeriod, workerDuties.signalHeartbeat, resultHandler)

  }

  private def resultHandler(result: Either[Throwable, Unit]): Unit = {
    result match {
      case Left(t) => logger.error(s"Worker job failed", t)
      case _ => ()
    }
  }

  override def shutdown(): Unit = scheduler.shutdown
}

case class WorkerJobsConfig(threadPerCore: Int, runJobsPeriod: FiniteDuration, nodeStoreHeartbeat: Boolean,
                            heartBeatPeriod: FiniteDuration)
