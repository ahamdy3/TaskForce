package io.ahamdy.taskforce.leader

import java.util.concurrent.{Executors, TimeUnit}

import io.ahamdy.taskforce.common.Logging
import monix.execution.ExecutionModel.BatchedExecution
import monix.execution.internal.Platform
import monix.execution.{Scheduler, UncaughtExceptionReporter}
import monix.execution.schedulers.AsyncScheduler

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

trait LeaderJobs {
  def start(): Unit
  def shutdown(): Unit
}

class LeaderJobsImpl(config: LeaderJobsConfig, leaderDuties: LeaderDuties) extends LeaderJobs with Logging {

  val scheduledExecutor = Executors.newScheduledThreadPool(7)
  val scheduler: Scheduler = AsyncScheduler(
    Executors.newScheduledThreadPool(7),
    ExecutionContext.fromExecutor(scheduledExecutor),
    UncaughtExceptionReporter.LogExceptionsToStandardErr,
    BatchedExecution(1024))
  //new SchedulerImpl(SchedulerConfig(threadPoolSize = 7))

  override def start(): Unit = ??? /*{
    scheduler.scheduleAtFixedRate(0, config.leaderElectionPeriod.toSeconds, TimeUnit.SECONDS, leaderDuties.refreshJobsSchedule().)
      //.unsafeSchedule(config.leaderElectionPeriod, leaderDuties.electClusterLeader, resultHandler)
    scheduler.unsafeSchedule(config.refreshJobsSchedulePeriod, leaderDuties.refreshJobsSchedule(), resultHandler)
    scheduler.unsafeSchedule(config.refreshQueuedJobsPeriod, leaderDuties.refreshQueuedJobs, resultHandler)
    scheduler.unsafeSchedule(config.assignQueuedJobsPeriod, leaderDuties.assignQueuedJobs, resultHandler)
    scheduler.unsafeSchedule(config.cleanJobsPeriod, leaderDuties.cleanDeadNodesJobs(), resultHandler)
    scheduler.unsafeSchedule(config.queueScheduleJobsPeriod, leaderDuties.queueScheduledJobs, resultHandler)
    // scheduler.unsafeSchedule(config.scaleClusterPeriod, leaderDuties.scaleCluster, resultHandler)
  }*/

  override def shutdown(): Unit = ??? // scheduler.shutdown

  private def resultHandler(result: Either[Throwable, Unit]): Unit = {
    result match {
      case Left(t) => logger.error(s"Leader job failed", t)
      case _ => ()
    }
  }

}

case class LeaderJobsConfig(leaderElectionPeriod: FiniteDuration, refreshJobsSchedulePeriod: FiniteDuration,
                            refreshQueuedJobsPeriod: FiniteDuration, assignQueuedJobsPeriod: FiniteDuration,
                            cleanJobsPeriod: FiniteDuration, scaleClusterPeriod: FiniteDuration,
                            queueScheduleJobsPeriod: FiniteDuration)
