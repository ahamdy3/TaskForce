package io.ahamdy.taskforce.api

import cats.effect.IO
import io.ahamdy.taskforce.domain.NodeId

trait CloudManager {
  def scaleUp(nodesCount: Int): IO[Unit]
  def scaleDown(nodeIds: Set[NodeId]): IO[Unit]
}
