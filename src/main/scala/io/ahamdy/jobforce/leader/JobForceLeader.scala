package io.ahamdy.jobforce.leader

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

import fs2.Task
import fs2.interop.cats._
import cats.syntax.flatMap._
import io.ahamdy.jobforce.common.Logging
import io.ahamdy.jobforce.db.{CurrentNodesStore, JobsStore}
import io.ahamdy.jobforce.domain._
import io.ahamdy.jobforce.shared.NodeInfoProvider

import scala.collection.JavaConverters._

trait JobForceLeader {
  def isLeader: Boolean
  def electClusterLeader: Task[Unit]

  def queueJob(): Task[Unit]
  def assignJob(): Task[Unit]

  def cleanJobs(): Task[Unit]

  def scaleCluster: Task[Unit]
}

class JobForceLeaderImpl(nodeInfoProvider: NodeInfoProvider, currentNodesStore: CurrentNodesStore, jobsStore: JobsStore)
  extends JobForceLeader with Logging{
  val leaderFlag = new AtomicBoolean(false)
  val queuedJobs = new ConcurrentHashMap[JobLock, QueuedJobInstance]
  val runningJobs = new ConcurrentHashMap[JobLock, RunningJobInstance]

  override def isLeader: Boolean = leaderFlag.get()

  override def electClusterLeader: Task[Unit] = {
    currentNodesStore.getLeaderNodeByGroupName(nodeInfoProvider.nodeGroupId).flatMap {
      case nodeId: NodeId if nodeId == nodeInfoProvider.nodeId =>
        jobsStore.getQueuedJobs.map(jobsList =>
          queuedJobs.putAll(jobsList.map(job => job.jobLock -> job).toMap.asJava)) >>
        jobsStore.getRunningJobs.map(jobsList =>
          runningJobs.putAll(jobsList.map(job => job.jobLock -> job).toMap.asJava)) >>
        Task.delay(leaderFlag.set(true)) >>
        logInfo(s"Node $nodeId has been elected as a leader")
      case _ => Task.now(())
    }
  }

  override def queueJob(): Task[Unit] = ???
  override def assignJob(): Task[Unit] = ???

  override def cleanJobs(): Task[Unit] = ???

  override def scaleCluster: Task[Unit] = ???
}

case class JobForceLeaderConfig(maxWeightPerNode: Int)