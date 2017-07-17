package io.ahamdy.jobforce.db

import java.time.ZonedDateTime

import cats.implicits._
import fs2.interop.cats._
import fs2.Task
import io.ahamdy.jobforce.domain.{NodeGroupId, NodeId}

trait CurrentNodesStore {
  def updateCurrentNode(groupName: NodeGroupId, nodeId: NodeId, now: ZonedDateTime): Task[Unit]
  def getAllNodesByGroupName(groupName: NodeGroupId): Task[List[NodeId]]
  def getLeaderNodeByGroupName(groupName: NodeGroupId): Task[NodeId]
  def cleanDeadNodes(now: ZonedDateTime): Task[Unit]
}
