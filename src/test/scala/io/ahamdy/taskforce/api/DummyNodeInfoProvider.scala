package io.ahamdy.taskforce.api

import io.ahamdy.taskforce.domain.{NodeGroup, NodeId}

class DummyNodeInfoProvider(nodeName: String, groupName: String) extends NodeInfoProvider {
  override def nodeGroup: NodeGroup = NodeGroup(groupName)
  override def nodeId: NodeId = NodeId(nodeName)
}
