package io.ahamdy.taskforce.store

import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

import cats.syntax.flatMap._
import monix.eval.Task
import io.ahamdy.taskforce.common.Time
import io.ahamdy.taskforce.domain._

import scala.collection.JavaConverters._
import scala.collection.mutable

class DummyJobStore(time: Time) extends JobStore {
  val queuedJobStore = new ConcurrentHashMap[JobId, QueuedJob]()
  val runningJobStore = new ConcurrentHashMap[JobLock, RunningJob]()
  val finishedJobStore = new mutable.ArrayBuffer[FinishedJob]()

  override def getJobLastRunTime(id: JobId): Task[Option[ZonedDateTime]] =
    if (queuedJobStore.containsKey(id) || runningJobStore.containsKey(id))
      time.now.map(Some(_))
    else
      finishedJobStore.toList.map(_.finishTime) match {
        case Nil => Task.pure(None)
        case times if times.length > 1 => Task(Some(times.maxBy(_.getNano)))
      }

  override def getQueuedJobsOrderedByPriorityAndTime: Task[List[QueuedJob]] =
    Task(queuedJobStore.values().asScala.toList.sortBy(job => (job.priority.value, job.queuingTime.toEpochSecond)))

  override def moveQueuedJobToRunningJob(runningJob: RunningJob): Task[Unit] =
    if (Option(runningJobStore.putIfAbsent(runningJob.lock, runningJob)).isEmpty)
      Task(queuedJobStore.remove(runningJob.id)).map(_ => ())
    else
      Task.raiseError(new Exception("failed to move queued job to running job"))

  override def getRunningJobs: Task[List[RunningJob]] =
    Task(runningJobStore.values().asScala.toList)

  override def createQueuedJob(queuedJob: QueuedJob): Task[Boolean] =
    Task(Option(queuedJobStore.putIfAbsent(queuedJob.id, queuedJob)).isEmpty)

  override def getFinishedJobs: Task[List[FinishedJob]] =
    Task(finishedJobStore.toList)

  override def moveRunningJobToQueuedJob(queuedJob: QueuedJob): Task[Unit] =
    Task(runningJobStore.remove(queuedJob.lock)) >>
      Task {
        if (Option(queuedJobStore.putIfAbsent(queuedJob.id, queuedJob)).isEmpty)
          ()
        else
          new Exception("failed to move queued job to running job")
      }


  override def moveRunningJobToFinishedJob(finishedJob: FinishedJob): Task[Unit] =
    Task(runningJobStore.remove(finishedJob.lock)) >>
      Task(finishedJobStore.append(finishedJob))

  override def getRunningJobsByNodeId(nodeId: NodeId): Task[List[RunningJob]] =
    getRunningJobs.map(_.filter(_.nodeId == nodeId))

  def reset(): Unit = {
    queuedJobStore.clear()
    runningJobStore.clear()
    finishedJobStore.clear()
  }

  def isEmpty: Boolean = queuedJobStore.isEmpty && runningJobStore.isEmpty && finishedJobStore.isEmpty

  override def getRunningJobsByGroupName(nodeGroup: NodeGroup): Task[List[RunningJob]] =
    getRunningJobs.map(_.filter(job => job.nodeGroup == nodeGroup))
}
