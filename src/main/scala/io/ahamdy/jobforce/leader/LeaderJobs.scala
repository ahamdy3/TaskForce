package io.ahamdy.jobforce.leader

import io.ahamdy.jobforce.common.Logging
import io.ahamdy.jobforce.scheduling.{Scheduler, SchedulerConfig, SchedulerImpl}

import scala.concurrent.duration.FiniteDuration

trait LeaderJobs {
  def scheduler: Scheduler
  def start(): Unit
  def shutdown(): Unit
}

class LeaderJobsImpl(config: LeaderJobsConfig, leaderDuties: LeaderDuties) extends LeaderJobs with Logging {

  override val scheduler: Scheduler = new SchedulerImpl(SchedulerConfig(threadPoolSize = 6))

  override def start(): Unit = {
    scheduler.unsafeSchedule(config.leaderElectionPeriod, leaderDuties.electClusterLeader, resultHandler)
    scheduler.unsafeSchedule(config.refreshJobsSchedulePeriod, leaderDuties.refreshJobsSchedule, resultHandler)
    scheduler.unsafeSchedule(config.refreshQueuedJobsPeriod, leaderDuties.refreshQueuedJobs, resultHandler)
    scheduler.unsafeSchedule(config.assignQueuedJobsPeriod, leaderDuties.assignQueuedJobs, resultHandler)
    scheduler.unsafeSchedule(config.cleanJobsPeriod, leaderDuties.cleanJobs, resultHandler)
    scheduler.unsafeSchedule(config.queueScheduleJobsPeriod, leaderDuties.queueScheduledJobs, resultHandler)
    // scheduler.unsafeSchedule(config.scaleClusterPeriod, leaderDuties.scaleCluster, resultHandler)
  }

  override def shutdown(): Unit = scheduler.shutdown

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
