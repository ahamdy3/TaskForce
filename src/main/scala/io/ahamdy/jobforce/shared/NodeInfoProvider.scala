package io.ahamdy.jobforce.shared

import io.ahamdy.jobforce.domain.{NodeGroup, NodeId}

trait NodeInfoProvider {
  def nodeId: NodeId
  def nodeGroup: NodeGroup
}
