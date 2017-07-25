package io.ahamdy.jobforce.db

import cats.implicits._
import fs2.interop.cats._
import fs2.Task
import io.ahamdy.jobforce.domain.{JobNode, NodeGroup, NodeId}

sealed trait NodeStore {
  def getAllNodesByGroup(groupName: NodeGroup): Task[List[JobNode]]
  def getAllNodes: Task[List[JobNode]]
  def getLeaderNodeByGroup(nodeGroup: NodeGroup): Task[JobNode]
  def updateHeartbeat(nodeGroup: NodeGroup, nodeId: NodeId): Task[Unit] = Task.now(())
}
