package io.ahamdy.taskforce.leader

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import fs2.Task
import fs2.interop.cats._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.ahamdy.taskforce.api.{CloudManager, NodeInfoProvider}
import io.ahamdy.taskforce.domain._
import io.ahamdy.taskforce.scheduling.JobsScheduleProvider
import io.ahamdy.taskforce.store.{JobsStore, NodeStore}
import io.ahamdy.taskforce.implicits._
import io.ahamdy.taskforce.leader.components.{JobDueChecker, NodeLoadBalancer}
import io.ahamdy.taskforce.common.{Logging, Time}

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
  def gracefulShutdown: Task[Boolean]
}

class LeaderDutiesImpl(config: TaskForceLeaderConfig, nodeInfoProvider: NodeInfoProvider,
                       jobsScheduleProvider: JobsScheduleProvider, nodeStore: NodeStore, jobsStore: JobsStore,
                       cloudManager: CloudManager, time: Time)
  extends LeaderDuties with Logging {

  val startTime: ZonedDateTime = time.unsafeNow()
  val leaderFlag = new AtomicBoolean(false)
  val groupActiveFlag = new AtomicBoolean(true)
  val scheduledJobs: AtomicReference[List[ScheduledJob]] = new AtomicReference(List.empty)
  val queuedJobs = new ConcurrentHashMap[JobId, QueuedJob]()
  val runningJobs = new ConcurrentHashMap[JobLock, RunningJob]()

  override def isLeader: Boolean = leaderFlag.get()

  override def electClusterLeader: Task[Unit] =
    if (!leaderFlag.get())
      time.now.flatMap { now =>
        getLeaderNode.flatMap {
          case Some(node) if node.nodeId == nodeInfoProvider.nodeId && node.startTime.durationBetween(now) < config.youngestLeaderAge =>
            logInfo(s"Oldest node still too young to be a leader, current age: ${node.startTime.durationBetween(now)}, leader must be older than ${config.youngestLeaderAge}")
          case Some(node) if node.nodeId == nodeInfoProvider.nodeId =>
            jobsStore.getQueuedJobsOrderedByPriorityAndTime.map(jobsList =>
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
    else
      nodeStore.getAllNodes.map(_.find(_.nodeId == nodeInfoProvider.nodeId)).flatMap {
        case Some(leaderNode) if !leaderNode.active.value => gracefulShutdown.as(())
      }

  override def refreshJobsSchedule(ignoreLeader: Boolean = false): Task[Unit] = onlyIfLeader(ignoreLeader) {
    jobsScheduleProvider.getJobsSchedule.flatMap { jobs =>
      Task.delay(scheduledJobs.lazySet(jobs)) >>
        logDebug("jobs schedule has been refreshed")
    }
  }

  override def refreshQueuedJobs: Task[Unit] =
    onlyIfLeader {
      jobsStore.getQueuedJobsOrderedByPriorityAndTime.map(jobsList =>
        queuedJobs.putAll(jobsList.map(job => job.id -> job).toMap.asJava))
    }

  override def queueScheduledJobs: Task[Unit] = onlyIfLeader {
    sequenceUnit(scheduledJobs.get().map(queueScheduledJob))
  }

  override def assignQueuedJobs: Task[Unit] = onlyIfLeader {
    queuedJobs.values().asScala.toList.sortBy(job => (job.priority.value, job.id.value))
      .foldLeft(Task.now(0))(assignJob).as(Task.unit)
  }

  def queueScheduledJob(job: ScheduledJob): Task[Unit] =
    if (!runningJobs.containsKey(job.lock) && !queuedJobs.containsKey(job.id))
      time.now.flatMap { now =>
        jobsStore.getJobLastRunTime(job.id).map(_.getOrElse(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")))).flatMap {
          case lastRunTime if JobDueChecker.isDue(job.schedule, now, lastRunTime) =>
            val queuedJob = job.toQueuedJob(now)
            Task.delay(queuedJobs.putIfAbsent(queuedJob.id, queuedJob)) >>
              jobsStore.createQueuedJob(queuedJob).as(())
          case _ => Task.unit
        }
      }
    else
      Task.unit

  def assignJob(waitingCount: Task[Int], queuedJob: QueuedJob): Task[Int] =
    if (groupActiveFlag.get())
      time.now.flatMap { now =>
        waitingCount.flatMap { counter =>
          nodeStore.getAllActiveNodesByGroup(nodeInfoProvider.nodeGroup).flatMap {
            case allActiveNodes if allActiveNodes.length >= config.minActiveNodes =>
              NodeLoadBalancer.leastLoadedNode(runningJobs.values().asScala.toList, allActiveNodes, queuedJob.versionRule,
                nodeInfoProvider.nodeId, config.leaderAlsoWorker, counter) match {
                case Some(nodeLoad) if NodeLoadBalancer.canNodeHandle(nodeLoad.jobsWeight, queuedJob.weight.value, config.maxWeightPerNode) =>
                  val runningJob = queuedJob.toRunningJobAndIncAttempts(nodeLoad.node.nodeId, now)
                  jobsStore.moveQueuedJobToRunningJob(runningJob) >>
                    Task.delay(queuedJobs.remove(runningJob.id)) >>
                    Task.delay(runningJobs.put(runningJob.lock, runningJob)).as(counter)
                case Some(nodeLoad) =>
                  logInfo(s"The least loaded node ${nodeLoad.node.nodeId} has already loaded with ${nodeLoad.jobsWeight} so it can't take ${queuedJob.id}")
                    .as(counter + 1)
                case None =>
                  logInfo(s"There' no available active nodes with required version to handle ${queuedJob.id}").as(counter)
              }
            case allActiveNodes =>
              logInfo(s"Cluster is not ready, current active nodes: ${allActiveNodes.length}, min active nodes: ${config.minActiveNodes}")
                .as(counter)
          }
        }
      }
    else
      logInfo(s"Node group ${nodeInfoProvider.nodeGroup} has been marked as inactive, skipping ${queuedJob.id}").as(0)

  def cleanJob(runningJob: RunningJob): Task[Unit] =
    if (runningJob.attempts.attempts < runningJob.attempts.maxAttempts.value)
      for {
        now <- time.now
        queuedJob <- Task.now(runningJob.toQueuedJob(now))
        _ <- jobsStore.moveRunningJobToQueuedJob(queuedJob)
        _ <- Task.delay(queuedJobs.put(queuedJob.id, queuedJob))
        _ <- Task.delay(runningJobs.remove(runningJob.lock))
      } yield ()
    else
      for {
        now <- time.now
        _ <- jobsStore.moveRunningJobToFinishedJob(runningJob.toFinishedJob(now, JobResult.Failure,
          Some(JobResultMessage(s"${runningJob.nodeId} is dead and max attempts has been reached"))))
        _ <- Task.delay(runningJobs.remove(runningJob.lock))
      } yield ()

  def cleanJobs(runningJob: List[RunningJob]): Task[Unit] =
    sequenceUnit(runningJob.map(cleanJob))

  override def cleanDeadNodesJobs(ignoreLeader: Boolean = false): Task[Unit] = onlyIfLeader(ignoreLeader) {
    nodeStore.getAllNodes.flatMap(allNodes => cleanJobs(getDeadRunningJobs(allNodes.map(_.nodeId).toSet)))
  }

  def getLeaderNode: Task[Option[JobNode]] =
    nodeStore.getAllNodesByGroup(nodeInfoProvider.nodeGroup).map { nodes =>
      nodes.filter(_.active.value) match {
        case Nil => None
        case activeNodes => Some(activeNodes.minBy(node => (node.startTime.toEpochSecond, node.nodeId.value)))
      }
    }

  override def scaleCluster: Task[Unit] = onlyIfLeader {
    ???
  }

  override def gracefulShutdown: Task[Boolean] =
    if (groupActiveFlag.get())
      for {
        _ <- nodeStore.updateGroupNodesStatus(nodeInfoProvider.nodeGroup, NodeActive(false))
        _ <- Task.delay(groupActiveFlag.set(false))
        readyToShutdown <-
        if (isLeader) Task.delay(runningJobs.isEmpty)
        else jobsStore.getRunningJobsGroupName(nodeInfoProvider.nodeGroup).map(_.isEmpty)
      } yield readyToShutdown
    else
      Task.delay(runningJobs.isEmpty)

  def getDeadRunningJobs(allNodes: Set[NodeId]): List[RunningJob] =
    runningJobs.values().asScala.toList.filterNot(job => allNodes.contains(job.nodeId))

  private def onlyIfLeader(task: Task[Unit]): Task[Unit] =
    if (isLeader) task else Task.unit

  private def onlyIfLeader(ignore: Boolean)(task: Task[Unit]): Task[Unit] =
    if (ignore || isLeader) task else Task.unit
}

case class TaskForceLeaderConfig(minActiveNodes: Int,
                                 maxWeightPerNode: Int,
                                 youngestLeaderAge: FiniteDuration,
                                 leaderAlsoWorker: Boolean)