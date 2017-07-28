package io.ahamdy.jobforce.store

import fs2.Task
import io.ahamdy.jobforce.domain.{JobNode, NodeGroup, NodeId}

trait NodeStore {
  def getAllNodesByGroup(groupName: NodeGroup): Task[List[JobNode]]
  def getAllNodes: Task[List[JobNode]]
  def getLeaderNodeByGroup(nodeGroup: NodeGroup): Task[JobNode]
  def updateHeartbeat(nodeGroup: NodeGroup, nodeId: NodeId): Task[Unit] = Task.now(())
}
