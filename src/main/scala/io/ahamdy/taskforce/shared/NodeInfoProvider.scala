package io.ahamdy.taskforce.shared

import io.ahamdy.taskforce.domain.{NodeGroup, NodeId}

trait NodeInfoProvider {
  def nodeId: NodeId
  def nodeGroup: NodeGroup
}
