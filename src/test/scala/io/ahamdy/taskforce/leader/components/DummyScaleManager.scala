package io.ahamdy.taskforce.leader.components

import java.util.concurrent.atomic.AtomicInteger

import fs2.Task
import io.ahamdy.taskforce.domain.NodeId
import io.ahamdy.taskforce.syntax.task._

class DummyScaleManager extends ScaleManager {
  val lastReportedQueuedAndRunningWeights = new AtomicInteger(0)
  val lastReportedActiveNodesCapacity = new AtomicInteger(0)

  override def scaleCluster(queuedAndRunningWeights: Int, activeNodesCapacity: Int): Task[Unit] = Task.delay {
    lastReportedQueuedAndRunningWeights.set(queuedAndRunningWeights)
    lastReportedActiveNodesCapacity.set(activeNodesCapacity)
  }

  override def cleanInactiveNodes(currentNodesRunningJobs: Set[NodeId]): Task[Unit] = Task.unit

  def reset(): Unit = {
    lastReportedQueuedAndRunningWeights.set(0)
    lastReportedActiveNodesCapacity.set(0)
  }
}
