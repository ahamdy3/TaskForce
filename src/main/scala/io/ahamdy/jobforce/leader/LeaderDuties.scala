package io.ahamdy.jobforce.leader

import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

import fs2.Task
import fs2.interop.cats._
import cats.implicits._
import io.ahamdy.jobforce.common.{Logging, Time}
import io.ahamdy.jobforce.db.{CurrentNodesStore, JobsStore}
import io.ahamdy.jobforce.domain._
import io.ahamdy.jobforce.scheduling.JobsScheduleProvider
import io.ahamdy.jobforce.shared.NodeInfoProvider
import io.ahamdy.jobforce.syntax._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

trait LeaderDuties {
  def isLeader: Boolean

  def electClusterLeader: Task[Unit]

  def refreshQueuedJobs: Task[Unit]

  def refreshJobsSchedule: Task[Unit]

  def queueScheduledJobs: Task[Unit]

  def assignQueuedJobs: Task[Unit]

  def cleanJobs: Task[Unit]

  def scaleCluster: Task[Unit]
}

class LeaderDutiesImpl(config: JobForceLeaderConfig, nodeInfoProvider: NodeInfoProvider, jobsScheduleProvider: JobsScheduleProvider,
                       currentNodesStore: CurrentNodesStore, jobsStore: JobsStore, time: Time)
  extends LeaderDuties with Logging {
  val startTime = time.unsafeNow()
  val leaderFlag = new AtomicBoolean(false)
  var jobsSchedule: List[ScheduledJob] = List.empty
  val queuedJobs = new ConcurrentHashMap[JobId, QueuedJob]
  val runningJobs = new ConcurrentHashMap[JobLock, RunningJob]

  override def isLeader: Boolean = leaderFlag.get()

  override def electClusterLeader: Task[Unit] =
    time.now.flatMap { now =>
      currentNodesStore.getLeaderNodeByGroup(nodeInfoProvider.nodeGroup).flatMap {
        case node if node.nodeId == nodeInfoProvider.nodeId && node.startTime.minus(now) < config.youngestLeaderAge =>
          logInfo(s"Oldest node still too young to be a leader, current age: ${node.startTime.minus(now)}, leader must be older than ${config.youngestLeaderAge}")
        case node if node.nodeId == nodeInfoProvider.nodeId =>
          jobsStore.getQueuedJobs.map(jobsList =>
            queuedJobs.putAll(jobsList.map(job => job.id -> job).toMap.asJava)) >>
            jobsStore.getRunningJobs.map(jobsList =>
              runningJobs.putAll(jobsList.map(job => job.lock -> job).toMap.asJava)) >>
            Task.delay(leaderFlag.set(true)) >>
            logInfo(s"Node ${node.nodeId} has been elected as a leader")
        case _ => Task.now(())
      }
    }

  override def refreshJobsSchedule: Task[Unit] = onlyIfLeader {
    jobsScheduleProvider.getJobsSchedule.flatMap { jobs =>
      Task.delay(jobsSchedule = jobs) >>
        logDebug("jobs schedule has been refreshed")
    }
  }

  override def refreshQueuedJobs: Task[Unit] =
    onlyIfLeader {
      jobsStore.getQueuedJobs.map(jobsList =>
        queuedJobs.putAll(jobsList.map(job => job.id -> job).toMap.asJava))
    }

  override def queueScheduledJobs: Task[Unit] = onlyIfLeader {
    jobsSchedule.map(queueScheduledJob).sequence.map(_ => ())
  }

  override def assignQueuedJobs: Task[Unit] = onlyIfLeader {
    queuedJobs.values().asScala.toList.map(assignJob).sequence.map(_ => ())
  }

  private def queueScheduledJob(job: ScheduledJob): Task[Unit] =
    for {
      now <- time.now
      lastRunTime <- jobsStore.getJobLastRunTime(job.lock).map(_.getOrElse(startTime))
      if isDue(job, now, lastRunTime)
      queuedJob = job.toQueuedJob(JobId.generateNew, now)
      _ <- Task.delay(queuedJobs.putIfAbsent(queuedJob.id, queuedJob))
      _ <- jobsStore.createQueuedJob(queuedJob)
    } yield ()

  private def isDue(scheduledJob: ScheduledJob, now: ZonedDateTime, lastRunTime: ZonedDateTime): Boolean =
    scheduledJob.schedule.cronLine.nextExecutionTimeAfter(lastRunTime)
      .isBetween(now.minus(scheduledJob.schedule.startTimeWindow), now)

  private def assignJob(queuedJobInstance: QueuedJob): Task[Unit] =
    time.now.flatMap { now =>
      currentNodesStore.getAllNodesByGroup(nodeInfoProvider.nodeGroup).map(allNodes =>
        leastLoadedNode(runningJobs.values().asScala.toList, allNodes) match {
          case nodeLoad if nodeLoad.jobsWeight < config.maxWeightPerNode =>
            val runningJobInstance = queuedJobInstance.toRunningJob(nodeLoad.node.nodeId, now)
            jobsStore.moveQueuedJobToRunningJob(runningJobInstance) >>
              Task.delay(queuedJobs.remove(runningJobInstance.id)) >>
              Task.delay(runningJobs.put(runningJobInstance.lock, runningJobInstance))
        }
      )
    }


  override def cleanJobs(): Task[Unit] =
    onlyIfLeader {
      currentNodesStore.getAllNodes.map { allNodes =>
        val allNodesSet = allNodes.map(_.nodeId).toSet
        runningJobs.values().asScala.toList.foreach {
          case runningJob if !allNodesSet.contains(runningJob.nodeId) &&
            runningJob.attempts.attempts < runningJob.attempts.maxAttempts =>
            time.now.flatMap { now =>
              val queuedJob = runningJob.toQueuedJob(now)
              jobsStore.moveRunningJobToQueuedJob(queuedJob) >>
                Task.delay(queuedJobs.put(queuedJob.id, queuedJob)) >>
                Task.delay(runningJobs.remove(queuedJob.lock))
            }
          case _ => ()
        }
      }
    }

  override def scaleCluster: Task[Unit] = onlyIfLeader {
    ???
  }

  def leastLoadedNode(runningJobs: List[RunningJob], allNodes: List[JobNode]): NodeLoad = {
    val nodeLoadOrdering = Ordering.by((x: NodeLoad) => (x.jobsWeight, x.node.nodeId.value))
    val nodesMap = allNodes.map(node => node.nodeId -> node).toMap

    val activeLoads = runningJobs.collect {
      case job if nodesMap.contains(job.nodeId) && nodesMap(job.nodeId).active.value =>
        NodeLoad(nodesMap(job.nodeId), job.weight.value)
    }

    val allNodesLoad = allNodes.map(node => NodeLoad(node, 0)).filter(_.node.active.value)

    combineNodeLoads(activeLoads, allNodesLoad)
      .min(nodeLoadOrdering)
  }

  private def combineNodeLoads(list1: List[NodeLoad], list2: List[NodeLoad]): List[NodeLoad] =
    (list1 ++ list2).groupBy(_.node).map { case (node, nodes) => NodeLoad(node, nodes.map(_.jobsWeight).sum) }.toList

  private def onlyIfLeader(task: Task[Unit]): Task[Unit] = if (isLeader) task else Task.now(())
}

case class JobForceLeaderConfig(maxWeightPerNode: Int, youngestLeaderAge: Duration)