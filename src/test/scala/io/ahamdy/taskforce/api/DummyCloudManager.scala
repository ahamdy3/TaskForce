package io.ahamdy.taskforce.api

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import monix.eval.Task
import cats.syntax.flatMap._
import io.ahamdy.taskforce.domain.NodeId

class DummyCloudManager(initialNodesCount: Int) extends CloudManager {

  val nodesCounter: AtomicInteger = new AtomicInteger(initialNodesCount)
  val scaledDownNodes: AtomicReference[Set[NodeId]] = new AtomicReference(Set.empty)

  override def scaleUp(nodesCount: Int): Task[Unit] =
    Task(nodesCounter.getAndAccumulate(nodesCount, _ + _))

  override def scaleDown(nodeIds: Set[NodeId]): Task[Unit] =
    Task(nodesCounter.getAndAccumulate(nodeIds.size, _ - _)) >>
      Task(scaledDownNodes.set(scaledDownNodes.get() ++ nodeIds))

  def reset: Unit = nodesCounter.set(initialNodesCount)
}
