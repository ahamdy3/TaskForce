package io.ahamdy.taskforce.api

import io.ahamdy.taskforce.domain.{NodeGroup, NodeId}

trait NodeInfoProvider {
  def nodeId: NodeId
  def nodeGroup: NodeGroup
}
