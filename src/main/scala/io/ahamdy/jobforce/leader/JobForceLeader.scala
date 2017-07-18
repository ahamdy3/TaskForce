package io.ahamdy.jobforce.leader

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

import fs2.Task
import fs2.interop.cats._
import cats.syntax.flatMap._
import io.ahamdy.jobforce.common.{Logging, Time}
import io.ahamdy.jobforce.db.{CurrentNodesStore, JobsStore}
import io.ahamdy.jobforce.domain._
import io.ahamdy.jobforce.shared.NodeInfoProvider

import scala.collection.JavaConverters._

trait JobForceLeader {
  def isLeader: Boolean

  def electClusterLeader: Task[Unit]

  def queueJob(queuedJobInstance: QueuedJobInstance): Task[Unit]

  def assignJob(queuedJobInstance: QueuedJobInstance): Task[Unit]

  def cleanJobs(): Task[Unit]

  def scaleCluster: Task[Unit]
}

class JobForceLeaderImpl(config: JobForceLeaderConfig, nodeInfoProvider: NodeInfoProvider,
                         currentNodesStore: CurrentNodesStore, jobsStore: JobsStore, time: Time)
  extends JobForceLeader with Logging {
  val leaderFlag = new AtomicBoolean(false)
  val queuedJobs = new ConcurrentHashMap[JobLock, QueuedJobInstance]
  val runningJobs = new ConcurrentHashMap[JobLock, RunningJobInstance]

  override def isLeader: Boolean = leaderFlag.get()

  override def electClusterLeader: Task[Unit] = {
    currentNodesStore.getLeaderNodeByGroup(nodeInfoProvider.nodeGroup).flatMap {
      case node if node.nodeId == nodeInfoProvider.nodeId =>
        jobsStore.getQueuedJobs.map(jobsList =>
          queuedJobs.putAll(jobsList.map(job => job.jobLock -> job).toMap.asJava)) >>
          jobsStore.getRunningJobs.map(jobsList =>
            runningJobs.putAll(jobsList.map(job => job.jobLock -> job).toMap.asJava)) >>
          Task.delay(leaderFlag.set(true)) >>
          logInfo(s"Node ${node.nodeId} has been elected as a leader")
      case _ => Task.now(())
    }
  }

  override def queueJob(queuedJobInstance: QueuedJobInstance): Task[Unit] = {
    if (!queuedJobs.containsKey(queuedJobInstance.jobLock) &&
      !runningJobs.containsKey(queuedJobInstance.jobLock))
      jobsStore.createQueuedJob(queuedJobInstance) >>
        Task.delay(queuedJobs.put(queuedJobInstance.jobLock, queuedJobInstance))
    else
      Task.now(())
  }

  override def assignJob(queuedJobInstance: QueuedJobInstance): Task[Unit] = {
    time.now.flatMap { now =>
      currentNodesStore.getAllNodesByGroup(nodeInfoProvider.nodeGroup).map(allNodes =>
        leastLoadedNode(runningJobs.values().asScala.toList, allNodes) match {
          case nodeLoad if nodeLoad.jobsWeight < config.maxWeightPerNode =>
            val runningJobInstance = queuedJobInstance.toRunningJobInstance(nodeLoad.node.nodeId, now)
            jobsStore.moveQueuedJobToRunningJob(runningJobInstance) >>
            Task.delay(queuedJobs.remove(runningJobInstance.jobLock)) >>
              Task.delay(runningJobs.put(runningJobInstance.jobLock, runningJobInstance))
        }
      )
    }
  }

  override def cleanJobs(): Task[Unit] = {
    currentNodesStore.getAllNodes.map{ allNodes =>
      val allNodesSet = allNodes.map(_.nodeId).toSet
      runningJobs.values().asScala.toList.foreach{
        case runningJob if !allNodesSet.contains(runningJob.nodeId) &&
          runningJob.jobAttempts.attempts < runningJob.jobAttempts.maxAttempts =>
          val queuedJob = runningJob.toQueuedJobInstance
          jobsStore.moveRunningJobToQueuedJob(queuedJob) >>
          Task.delay(queuedJobs.put(queuedJob.jobLock, queuedJob)) >>
          Task.delay(runningJobs.remove(queuedJob.jobLock))
        case _ => ()
      }
    }
  }

  override def scaleCluster: Task[Unit] = ???

  def leastLoadedNode(runningJobs: List[RunningJobInstance], allNodes: List[JobNode]): NodeLoad = {
    val nodeLoadOrdering = Ordering.by((x: NodeLoad) => (x.jobsWeight, x.node.nodeId.value))
    val nodesMap = allNodes.map(node => node.nodeId -> node).toMap

    val activeLoads = runningJobs.collect {
      case job if nodesMap.contains(job.nodeId) && nodesMap(job.nodeId).active.value =>
        NodeLoad(nodesMap(job.nodeId), job.jobWeight.value)
    }

    val allNodesLoad = allNodes.map(node => NodeLoad(node, 0)).filter(_.node.active.value)

    combineNodeLoads(activeLoads, allNodesLoad)
      .min(nodeLoadOrdering)
  }

  private def combineNodeLoads(list1: List[NodeLoad], list2: List[NodeLoad]): List[NodeLoad] =
    (list1 ++ list2).groupBy(_.node).map { case (node, nodes) => NodeLoad(node, nodes.map(_.jobsWeight).sum) }.toList
}

case class JobForceLeaderConfig(maxWeightPerNode: Int)