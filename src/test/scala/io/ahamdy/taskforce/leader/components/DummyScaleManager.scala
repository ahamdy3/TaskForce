package io.ahamdy.taskforce.leader.components

import java.util.concurrent.atomic.AtomicInteger

import cats.effect.IO
import io.ahamdy.taskforce.domain.NodeId
import io.ahamdy.taskforce.syntax.IO._

class DummyScaleManager extends ScaleManager {
  val lastReportedQueuedAndRunningWeights = new AtomicInteger(0)
  val lastReportedActiveNodesCapacity = new AtomicInteger(0)

  override def scaleCluster(queuedAndRunningWeights: Int, activeNodesCapacity: Int): IO[Unit] = IO {
    lastReportedQueuedAndRunningWeights.set(queuedAndRunningWeights)
    lastReportedActiveNodesCapacity.set(activeNodesCapacity)
  }

  override def cleanInactiveNodes(currentNodesRunningJobs: Set[NodeId]): IO[Unit] = IO.unit

  def reset(): Unit = {
    lastReportedQueuedAndRunningWeights.set(0)
    lastReportedActiveNodesCapacity.set(0)
  }
}
