package io.ahamdy.taskforce.store

import fs2.Task
import io.ahamdy.taskforce.domain.{JobNode, NodeActive, NodeGroup, NodeId}

trait NodeStore {
  def getAllNodes: Task[List[JobNode]]
  def updateNodeStatus(nodeId: NodeId, active: NodeActive): Task[Unit]
  def updateGroupNodesStatus(nodeGroup: NodeGroup, active: NodeActive): Task[Unit]

  def getAllNodesByGroup(groupName: NodeGroup): Task[List[JobNode]] =
    getAllNodes.map(_.filter(_.nodeGroup == groupName))
  def getAllActiveNodesByGroup(groupName: NodeGroup): Task[List[JobNode]] =
    getAllNodesByGroup(groupName).map(_.filter(_.active.value))
  def getAllActiveNodesCountByGroup(groupName: NodeGroup): Task[Int] =
    getAllActiveNodesByGroup(groupName).map(_.length)
  def getAllInactiveNodesByGroup(groupName: NodeGroup): Task[List[JobNode]] =
  getAllNodesByGroup(groupName).map(_.filterNot(_.active.value))
  def getYoungestActiveNodesByGroup(groupName: NodeGroup, count: Int = 1): Task[List[JobNode]] =
    getAllNodesByGroup(groupName).map(_.filter(_.active.value)
      .sortBy(node => (node.startTime.toInstant.getEpochSecond, node.nodeId.value)).takeRight(count))
  def getOldestActiveNonLeaderNodesByGroup(groupName: NodeGroup, count: Int = 1): Task[List[JobNode]] =
    getAllNodesByGroup(groupName).map(_.filter(_.active.value)
      .sortBy(node =>
        (node.startTime.toInstant.getEpochSecond, node.nodeId.value))(Ordering.Tuple2(Ordering.Long.reverse, Ordering.String))
      .drop(1).takeRight(count))
  def updateHeartbeat(nodeGroup: NodeGroup, nodeId: NodeId): Task[Unit] = Task.now(())
}
