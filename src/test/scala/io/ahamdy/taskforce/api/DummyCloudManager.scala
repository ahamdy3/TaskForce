package io.ahamdy.taskforce.api
import java.util.concurrent.atomic.AtomicInteger

import fs2.Task
import io.ahamdy.taskforce.domain.NodeId

class DummyCloudManager(initialNodesCount: Int) extends CloudManager {

  val nodesCounter: AtomicInteger = new AtomicInteger(initialNodesCount)

  override def scaleUp(nodesCount: Int): Task[Unit] =
    Task.delay(nodesCounter.getAndAccumulate(nodesCount, _ + _))
  override def scaleDown(nodeIds: Set[NodeId]): Task[Unit] =
    Task.delay(nodesCounter.getAndAccumulate(nodeIds.size, _ - _))

  def reset: Unit = nodesCounter.set(initialNodesCount)
}
