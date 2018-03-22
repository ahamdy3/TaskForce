package io.ahamdy.taskforce.api

import monix.eval.Task
import io.ahamdy.taskforce.domain.NodeId

trait CloudManager {
  def scaleUp(nodesCount: Int): Task[Unit]
  def scaleDown(nodeIds: Set[NodeId]): Task[Unit]
}
