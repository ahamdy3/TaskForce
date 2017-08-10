package io.ahamdy.taskforce.api

import fs2.Task
import io.ahamdy.taskforce.domain.NodeId

trait CloudManager {
  def scaleUp(nodesCount: Int): Task[Unit]
  def scaleDown(nodeIds: List[NodeId]): Task[Unit]
}
