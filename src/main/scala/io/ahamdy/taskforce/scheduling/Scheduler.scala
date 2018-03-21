package io.ahamdy.taskforce.scheduling

import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory}

import cats.effect.IO
import fs2.Stream

import scala.concurrent.duration.FiniteDuration
import cats.implicits._
import io.ahamdy.taskforce.common.Logging

trait Scheduler {
  def unsafeSchedule(period: FiniteDuration, task: IO[Unit], resultHandler: (Either[Throwable, Unit]) => Unit): Unit
  def shutdown: Unit
}

class SchedulerImpl(config: SchedulerConfig) extends Scheduler with Logging {

  private val executor: ScheduledExecutorService =
    Executors.newScheduledThreadPool(config.threadPoolSize, Executors.defaultThreadFactory())
  implicit val scheduler: fs2.Scheduler = fs2.Scheduler.fromScheduledExecutorService(executor)

  override def unsafeSchedule(period: FiniteDuration, task: IO[Unit], resultHandler: (Either[Throwable, Unit]) => Unit): Unit = {
    /*val neverFailingTask: IO[Unit] = task.attempt map {
      _.leftMap { err => logger.error(s"Unexpected error from a periodic task $err") }
    }

    (Stream.eval(neverFailingTask) ++ Stream.repeatEval(neverFailingTask.schedule(period))).run.unsafeRunAsync(resultHandler)*/
    ???
  }

  override def shutdown: Unit =
    executor.shutdown()
}

case class SchedulerConfig(threadPoolSize: Int)
