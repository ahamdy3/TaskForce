package io.ahamdy.taskforce.store

import java.util.concurrent.atomic.AtomicReference

import fs2.Task
import io.ahamdy.taskforce.common.Time
import io.ahamdy.taskforce.domain._

class DummyNodeStore(time: Time, defaultGroup: NodeGroup = NodeGroup("test-group-1")) extends NodeStore {
  val nodesList = new AtomicReference[List[JobNode]](List(
    JobNode(NodeId("test-node-1"), defaultGroup, time.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0")),
    JobNode(NodeId("test-node-2"), defaultGroup, time.unsafeNow(), NodeActive(true), NodeVersion("1.0.0")),
    JobNode(NodeId("test-node-3"), NodeGroup("test-group-2"), time.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0")) // other group
  ))

  override def getAllNodes: Task[List[JobNode]] = Task.delay(nodesList.get())

  override def updateGroupNodesStatus(nodeGroup: NodeGroup, active: NodeActive): Task[Unit] =
    Task.delay {
      val updatedList = nodesList.get().map(_.copy(active = NodeActive(false)))
      nodesList.set(updatedList)
    }

  def reset(): Unit = nodesList.set(List(
    JobNode(NodeId("test-node-1"), defaultGroup, time.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0")),
    JobNode(NodeId("test-node-2"), defaultGroup, time.unsafeNow(), NodeActive(true), NodeVersion("1.0.0")),
    JobNode(NodeId("test-node-3"), NodeGroup("test-group-2"), time.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0")) // other group
  ))

  override def updateNodeStatus(nodeId: NodeId, active: NodeActive): Task[Unit] = Task.delay(nodesList.set(
    nodesList.get().map{
      case node if node.nodeId == nodeId => node.copy(active = active)
      case node => node
    }
  ))
}
