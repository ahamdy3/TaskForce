package io.ahamdy.jobforce.shared

import io.ahamdy.jobforce.domain.{NodeGroupId, NodeId}

trait NodeInfoProvider {
  def nodeId: NodeId
  def nodeGroupId: NodeGroupId
}
