package io.ahamdy.taskforce.leader.components

import fs2.Task
import io.ahamdy.taskforce.domain.NodeId
import io.ahamdy.taskforce.syntax.task._

class DummyScaleManager extends ScaleManager {
  override def scaleCluster(queuedAndRunningWeights: Int, activeNodesCapacity: Int): Task[Unit] = Task.unit
  override def cleanInactiveNodes(currentNodesRunningJobs: Set[NodeId]): Task[Unit] = Task.unit
}
