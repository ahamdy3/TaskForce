package io.ahamdy.taskforce.store

import cats.effect.IO
import io.ahamdy.taskforce.domain.{JobNode, NodeActive, NodeGroup, NodeId}

trait NodeStore {
  def getAllNodes: IO[List[JobNode]]
  def updateNodeStatus(nodeId: NodeId, active: NodeActive): IO[Unit]
  def updateGroupNodesStatus(nodeGroup: NodeGroup, active: NodeActive): IO[Unit]

  def getAllNodesByGroup(groupName: NodeGroup): IO[List[JobNode]] =
    getAllNodes.map(_.filter(_.nodeGroup == groupName))
  def getAllActiveNodesByGroup(groupName: NodeGroup): IO[List[JobNode]] =
    getAllNodesByGroup(groupName).map(_.filter(_.active.value))
  def getAllActiveNodesCountByGroup(groupName: NodeGroup): IO[Int] =
    getAllActiveNodesByGroup(groupName).map(_.length)
  def getAllInactiveNodesByGroup(groupName: NodeGroup): IO[List[JobNode]] =
  getAllNodesByGroup(groupName).map(_.filterNot(_.active.value))
  def getYoungestActiveNodesByGroup(groupName: NodeGroup, count: Int = 1): IO[List[JobNode]] =
    getAllNodesByGroup(groupName).map(_.filter(_.active.value)
      .sortBy(node => (node.startTime.toInstant.getEpochSecond, node.nodeId.value)).takeRight(count))
  def getOldestActiveNonLeaderNodesByGroup(groupName: NodeGroup, count: Int = 1): IO[List[JobNode]] =
    getAllNodesByGroup(groupName).map(_.filter(_.active.value)
      .sortBy(node =>
        (node.startTime.toInstant.getEpochSecond, node.nodeId.value))(Ordering.Tuple2(Ordering.Long.reverse, Ordering.String))
      .drop(1).takeRight(count))
  def updateHeartbeat(nodeGroup: NodeGroup, nodeId: NodeId): IO[Unit] = IO.pure(())
}
