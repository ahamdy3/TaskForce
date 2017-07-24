package io.ahamdy.jobforce.db

import java.time.ZonedDateTime

import cats.implicits._
import fs2.interop.cats._
import fs2.Task
import io.ahamdy.jobforce.domain.{JobNode, NodeGroup, NodeId}

sealed trait NodeStore {
  def getAllNodesByGroup(groupName: NodeGroup): Task[List[JobNode]]
  def getAllNodes: Task[List[JobNode]]
  def getLeaderNodeByGroup(nodeGroup: NodeGroup): Task[JobNode]
}

trait PollBasedNodeStore extends NodeStore {
  def updateCurrentNode(nodeGroup: NodeGroup, nodeId: NodeId, now: ZonedDateTime): Task[Unit]
  def cleanDeadNodes(now: ZonedDateTime): Task[Unit]
}

trait PushBasedNodeStore extends NodeStore {
  def onNodeUp(nodeGroup: NodeGroup, nodeId: NodeId, now: ZonedDateTime): Task[Unit]
  def onNodeDown(nodeGroup: NodeGroup, nodeId: NodeId): Task[Unit]
}
