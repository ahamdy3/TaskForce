package io.ahamdy.taskforce.store

import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

import cats.syntax.flatMap._
import cats.effect.IO
import io.ahamdy.taskforce.common.Time
import io.ahamdy.taskforce.domain._

import scala.collection.JavaConverters._
import scala.collection.mutable

class DummyJobStore(time: Time) extends JobStore {
  val queuedJobStore = new ConcurrentHashMap[JobId, QueuedJob]()
  val runningJobStore = new ConcurrentHashMap[JobLock, RunningJob]()
  val finishedJobStore = new mutable.ArrayBuffer[FinishedJob]()

  override def getJobLastRunTime(id: JobId): IO[Option[ZonedDateTime]] =
    if (queuedJobStore.containsKey(id) || runningJobStore.containsKey(id))
      time.now.map(Some(_))
    else
      finishedJobStore.toList.map(_.finishTime) match {
        case Nil => IO.pure(None)
        case times if times.length > 1 => IO(Some(times.maxBy(_.getNano)))
      }

  override def getQueuedJobsOrderedByPriorityAndTime: IO[List[QueuedJob]] =
    IO(queuedJobStore.values().asScala.toList.sortBy(job => (job.priority.value, job.queuingTime.toEpochSecond)))

  override def moveQueuedJobToRunningJob(runningJob: RunningJob): IO[Unit] =
    if (Option(runningJobStore.putIfAbsent(runningJob.lock, runningJob)).isEmpty)
      IO(queuedJobStore.remove(runningJob.id)).map(_ => ())
    else
      IO.raiseError(new Exception("failed to move queued job to running job"))

  override def getRunningJobs: IO[List[RunningJob]] =
    IO(runningJobStore.values().asScala.toList)

  override def createQueuedJob(queuedJob: QueuedJob): IO[Boolean] =
    IO(Option(queuedJobStore.putIfAbsent(queuedJob.id, queuedJob)).isEmpty)

  override def getFinishedJobs: IO[List[FinishedJob]] =
    IO(finishedJobStore.toList)

  override def moveRunningJobToQueuedJob(queuedJob: QueuedJob): IO[Unit] =
    IO(runningJobStore.remove(queuedJob.lock)) >>
      IO {
        if (Option(queuedJobStore.putIfAbsent(queuedJob.id, queuedJob)).isEmpty)
          ()
        else
          new Exception("failed to move queued job to running job")
      }


  override def moveRunningJobToFinishedJob(finishedJob: FinishedJob): IO[Unit] =
    IO(runningJobStore.remove(finishedJob.lock)) >>
      IO(finishedJobStore.append(finishedJob))

  override def getRunningJobsByNodeId(nodeId: NodeId): IO[List[RunningJob]] =
    getRunningJobs.map(_.filter(_.nodeId == nodeId))

  def reset(): Unit = {
    queuedJobStore.clear()
    runningJobStore.clear()
    finishedJobStore.clear()
  }

  def isEmpty: Boolean = queuedJobStore.isEmpty && runningJobStore.isEmpty && finishedJobStore.isEmpty

  override def getRunningJobsByGroupName(nodeGroup: NodeGroup): IO[List[RunningJob]] =
    getRunningJobs.map(_.filter(job => job.nodeGroup == nodeGroup))
}
