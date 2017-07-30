package io.ahamdy.jobforce.leader

import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import fs2.Task
import fs2.interop.cats._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.ahamdy.jobforce.common.{Logging, Time}
import io.ahamdy.jobforce.domain._
import io.ahamdy.jobforce.scheduling.JobsScheduleProvider
import io.ahamdy.jobforce.shared.NodeInfoProvider
import io.ahamdy.jobforce.store.{JobsStore, NodeStore}
import io.ahamdy.jobforce.syntax._

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

trait LeaderDuties {
  def isLeader: Boolean
  def electClusterLeader: Task[Unit]
  def refreshQueuedJobs: Task[Unit]
  def refreshJobsSchedule(ignoreLeader: Boolean = false): Task[Unit]
  def queueScheduledJobs: Task[Unit]
  def assignQueuedJobs: Task[Unit]
  def cleanDeadNodesJobs(ignoreLeader: Boolean = false): Task[Unit]
  def scaleCluster: Task[Unit]
}

class LeaderDutiesImpl(config: JobForceLeaderConfig, nodeInfoProvider: NodeInfoProvider,
                       jobsScheduleProvider: JobsScheduleProvider, nodeStore: NodeStore, jobsStore: JobsStore, time: Time)
  extends LeaderDuties with Logging {

  val startTime: ZonedDateTime = time.unsafeNow()
  val leaderFlag = new AtomicBoolean(false)
  val scheduledJobs: AtomicReference[List[ScheduledJob]] = new AtomicReference(List.empty)
  val queuedJobs = new ConcurrentHashMap[JobId, QueuedJob]()
  val runningJobs = new ConcurrentHashMap[JobLock, RunningJob]()

  override def isLeader: Boolean = leaderFlag.get()

  override def electClusterLeader: Task[Unit] =
    time.now.flatMap { now =>
      getLeaderNode.flatMap {
        case node if node.nodeId == nodeInfoProvider.nodeId && node.startTime.minus(now) < config.youngestLeaderAge =>
          logInfo(s"Oldest node still too young to be a leader, current age: ${node.startTime.minus(now)}, leader must be older than ${config.youngestLeaderAge}")
        case node if node.nodeId == nodeInfoProvider.nodeId =>
          jobsStore.getQueuedJobsOrderedByPriority.map(jobsList =>
            queuedJobs.putAll(jobsList.map(job => job.id -> job).toMap.asJava)) >>
            jobsStore.getRunningJobs.map(jobsList =>
              runningJobs.putAll(jobsList.map(job => job.lock -> job).toMap.asJava)) >>
            cleanDeadNodesJobs(ignoreLeader = true) >>
            refreshJobsSchedule(ignoreLeader = true) >>
            Task.delay(leaderFlag.set(true)) >>
            logInfo(s"Node ${node.nodeId} has been elected as a leader")
        case _ => Task.unit
      }
    }

  override def refreshJobsSchedule(ignoreLeader: Boolean = false): Task[Unit] = onlyIfLeader(ignoreLeader) {
    jobsScheduleProvider.getJobsSchedule.flatMap { jobs =>
      Task.delay(scheduledJobs.lazySet(jobs)) >>
        logDebug("jobs schedule has been refreshed")
    }
  }

  override def refreshQueuedJobs: Task[Unit] =
    onlyIfLeader {
      jobsStore.getQueuedJobsOrderedByPriority.map(jobsList =>
        queuedJobs.putAll(jobsList.map(job => job.id -> job).toMap.asJava))
    }

  override def queueScheduledJobs: Task[Unit] = onlyIfLeader {
    sequenceUnit(scheduledJobs.get().map(queueScheduledJob))
  }

  override def assignQueuedJobs: Task[Unit] = onlyIfLeader {
    sequenceUnit(queuedJobs.values().asScala.toList.map(assignJob))
  }

  private def queueScheduledJob(job: ScheduledJob): Task[Unit] =
    if (!runningJobs.containsKey(job.lock) && !queuedJobs.containsKey(job.id))
      time.now.flatMap { now =>
        jobsStore.getJobLastRunTime(job.id).map(_.getOrElse(startTime)).flatMap {
          case lastRunTime if isDue(job, now, lastRunTime) =>
            val queuedJob = job.toQueuedJob(now)
            Task.delay(queuedJobs.putIfAbsent(queuedJob.id, queuedJob)) >>
              jobsStore.createQueuedJob(queuedJob).as(())
          case _ => Task.unit
        }
      }
    else
      Task.unit

  private def isDue(scheduledJob: ScheduledJob, now: ZonedDateTime, lastRunTime: ZonedDateTime): Boolean =
    scheduledJob.schedule.cronLine.nextExecutionTimeAfter(lastRunTime)
      .isBetween(now.minus(scheduledJob.schedule.startTimeWindow), now)

  private def assignJob(queuedJob: QueuedJob): Task[Unit] =
    time.now.flatMap { now =>
      nodeStore.getAllActiveNodesByGroup(nodeInfoProvider.nodeGroup).map {
        case allActiveNodes if allActiveNodes.length >= config.minActiveNodes =>
          leastLoadedNode(runningJobs.values().asScala.toList, allActiveNodes, queuedJob.versionRule) match {
            case Some(nodeLoad) if (nodeLoad.jobsWeight + queuedJob.weight.value) < config.maxWeightPerNode =>
              val runningJobInstance = queuedJob.toRunningJobAndIncAttempts(nodeLoad.node.nodeId, now)
              jobsStore.moveQueuedJobToRunningJob(runningJobInstance) >>
                Task.delay(queuedJobs.remove(runningJobInstance.id)) >>
                Task.delay(runningJobs.put(runningJobInstance.lock, runningJobInstance))
            case Some(nodeLoad) =>
              logInfo(s"The least loaded node ${nodeLoad.node.nodeId} has already loaded with ${nodeLoad.jobsWeight} so it cant take ${queuedJob.id}")
            case None =>
              logInfo(s"There' no available active nodes with required version to handle ${queuedJob.id}")
          }
        case allActiveNodes =>
          logInfo(s"Cluster is not ready, current active nodes: ${allActiveNodes.length}, min active nodes: ${config.minActiveNodes}")
      }
    }

  def cleanJob(runningJob: RunningJob): Task[Unit] =
    if (runningJob.attempts.attempts < runningJob.attempts.maxAttempts.value)
      time.now.flatMap { now =>
        val queuedJob = runningJob.toQueuedJob(now)
        jobsStore.moveRunningJobToQueuedJob(queuedJob) >>
          Task.delay(queuedJobs.put(queuedJob.id, queuedJob)) >>
          Task.delay(runningJobs.remove(queuedJob.lock))
      }
    else
      time.now.flatMap { now =>
        jobsStore.moveRunningJobToFinishedJob(runningJob.toFinishedJob(now, JobResult.Failure,
          Some(JobResultMessage(s"${runningJob.nodeId} is dead and max attempts has been reached")))) >>
          Task.delay(runningJobs.remove(runningJob.lock))
      }

  def cleanJobs(runningJob: List[RunningJob]): Task[Unit] =
    sequenceUnit(runningJob.map(cleanJob))

  override def cleanDeadNodesJobs(ignoreLeader: Boolean = false): Task[Unit] = onlyIfLeader(ignoreLeader) {
    nodeStore.getAllNodes.flatMap(allNodes => cleanJobs(getDeadRunningJobs(allNodes.map(_.nodeId).toSet)))
  }

  def getLeaderNode: Task[JobNode] =
    nodeStore.getAllNodesByGroup(nodeInfoProvider.nodeGroup).map { nodes =>
      nodes.filter(_.active.value).minBy(node => (node.startTime.toEpochSecond, node.nodeId.value))
    }

  override def scaleCluster: Task[Unit] = onlyIfLeader {
    ???
  }

  def leastLoadedNode(runningJobs: List[RunningJob], allActiveNodes: List[JobNode], versionRule: JobVersionRule): Option[NodeLoad] = {
    val nodeLoadOrdering = Ordering.by((x: NodeLoad) => (x.jobsWeight, x.node.nodeId.value))
    val activeNodesMap = allActiveNodes.map(node => node.nodeId -> node).toMap

    val activeLoads = runningJobs.collect {
      case job if activeNodesMap.contains(job.nodeId) =>
        NodeLoad(activeNodesMap(job.nodeId), job.weight.value)
    }

    val allNodesLoad = allActiveNodes.map(node => NodeLoad(node, 0))

    val (activeWorkerLoads, allWorkersLoad) =
      if (config.leaderAlsoWorker)
        (activeLoads, allNodesLoad)
      else
        (activeLoads.filterNot(_.node.nodeId == nodeInfoProvider.nodeId),
          allNodesLoad.filterNot(_.node.nodeId == nodeInfoProvider.nodeId))


    combineNodeLoads(activeWorkerLoads, allWorkersLoad).filter(checkNodeVersion(versionRule, _)) match {
      case Nil => None
      case nodes => Some(nodes.min(nodeLoadOrdering))
    }
  }

  def checkNodeVersion(versionRule: JobVersionRule, nodeLoad: NodeLoad): Boolean =
    versionRule match {
      case JobVersionRule(VersionRuleDirective.AnyVersion, _) => true
      case JobVersionRule(VersionRuleDirective.Exactly, jobVersion) => nodeLoad.node.version == jobVersion
      case JobVersionRule(VersionRuleDirective.AtLeast, jobVersion) => nodeLoad.node.version.value >= jobVersion.value
      case JobVersionRule(VersionRuleDirective.AtMost, jobVersion) => nodeLoad.node.version.value <= jobVersion.value
    }

  def getDeadRunningJobs(allNodes: Set[NodeId]): List[RunningJob] =
    runningJobs.values().asScala.toList.filterNot(job => allNodes.contains(job.nodeId))

  private def combineNodeLoads(list1: List[NodeLoad], list2: List[NodeLoad]): List[NodeLoad] =
    (list1 ++ list2).groupBy(_.node).map { case (node, nodes) => NodeLoad(node, nodes.map(_.jobsWeight).sum) }.toList

  private def onlyIfLeader(task: Task[Unit]): Task[Unit] =
    if (isLeader) task else Task.unit

  private def onlyIfLeader(ignore: Boolean)(task: Task[Unit]): Task[Unit] =
    if (ignore || isLeader) task else Task.unit
}

case class JobForceLeaderConfig(minActiveNodes: Int,
                                maxWeightPerNode: Int,
                                youngestLeaderAge: FiniteDuration,
                                leaderAlsoWorker: Boolean)