package io.ahamdy.jobforce.db

import java.time.ZonedDateTime

import cats.implicits._
import fs2.interop.cats._
import fs2.Task
import io.ahamdy.jobforce.domain.{JobNode, NodeGroup, NodeId}

trait CurrentNodesStore {
  def updateCurrentNode(nodeGroup: NodeGroup, nodeId: NodeId, now: ZonedDateTime): Task[Unit]
  def getAllNodesByGroup(groupName: NodeGroup): Task[List[JobNode]]
  def getAllNodes: Task[List[JobNode]]
  def getLeaderNodeByGroup(nodeGroup: NodeGroup): Task[JobNode]
  def cleanDeadNodes(now: ZonedDateTime): Task[Unit]
}
