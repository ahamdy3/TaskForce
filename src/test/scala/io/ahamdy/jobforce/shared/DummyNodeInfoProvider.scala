package io.ahamdy.jobforce.shared

import io.ahamdy.jobforce.domain.{NodeGroup, NodeId}

class DummyNodeInfoProvider(nodeName: String, groupName: String) extends NodeInfoProvider {
  override def nodeGroup: NodeGroup = NodeGroup(groupName)
  override def nodeId: NodeId = NodeId(nodeName)
}
