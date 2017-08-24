package io.ahamdy.taskforce.api

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import fs2.Task
import fs2.interop.cats._
import cats.syntax.flatMap._
import io.ahamdy.taskforce.domain.NodeId

class DummyCloudManager(initialNodesCount: Int) extends CloudManager {

  val nodesCounter: AtomicInteger = new AtomicInteger(initialNodesCount)
  val scaledDownNodes: AtomicReference[Set[NodeId]] = new AtomicReference(Set.empty)

  override def scaleUp(nodesCount: Int): Task[Unit] =
    Task.delay(nodesCounter.getAndAccumulate(nodesCount, _ + _))

  override def scaleDown(nodeIds: Set[NodeId]): Task[Unit] =
    Task.delay(nodesCounter.getAndAccumulate(nodeIds.size, _ - _)) >>
      Task.delay(scaledDownNodes.set(scaledDownNodes.get() ++ nodeIds))

  def reset: Unit = nodesCounter.set(initialNodesCount)
}
