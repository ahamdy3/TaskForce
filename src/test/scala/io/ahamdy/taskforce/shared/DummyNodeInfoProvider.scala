package io.ahamdy.taskforce.shared

import io.ahamdy.taskforce.api.NodeInfoProvider
import io.ahamdy.taskforce.domain.{NodeGroup, NodeId}

class DummyNodeInfoProvider(nodeName: String, groupName: String) extends NodeInfoProvider {
  override def nodeGroup: NodeGroup = NodeGroup(groupName)
  override def nodeId: NodeId = NodeId(nodeName)
}
