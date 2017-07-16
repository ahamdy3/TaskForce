package io.ahamdy.jobforce.scheduling

import java.util.concurrent.{Executors, ScheduledExecutorService}

import fs2.{Strategy, Stream, Task}

import scala.concurrent.duration.FiniteDuration
import cats.implicits._
import io.ahamdy.jobforce.common.Logging

trait Scheduler {
  def unsafeSchedule(period: FiniteDuration, task: Task[Unit], resultHandler: (Either[Throwable, Unit]) => Unit): Unit
  def shutdown: Unit
}

class SchedulerImpl(config: SchedulerConfig) extends Scheduler with Logging {

  private val executor: ScheduledExecutorService =
    Executors.newScheduledThreadPool(config.threadPoolSize, Strategy.daemonThreadFactory("Non-Block"))
  implicit val strategy: Strategy = Strategy.fromExecutor(executor)
  implicit val scheduler: fs2.Scheduler = fs2.Scheduler.fromScheduledExecutorService(executor)

  override def unsafeSchedule(period: FiniteDuration, task: Task[Unit], resultHandler: (Either[Throwable, Unit]) => Unit): Unit = {
    val neverFailingTask: Task[Unit] = task.attempt map {
      _.leftMap { err => logger.error(s"Unexpected error from a periodic task $err") }
    }

    (Stream.eval(neverFailingTask) ++ Stream.repeatEval(neverFailingTask.schedule(period))).run.unsafeRunAsync(resultHandler)
  }

  override def shutdown: Unit =
    executor.shutdown()
}

case class SchedulerConfig(threadPoolSize: Int)
