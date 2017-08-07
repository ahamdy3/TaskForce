package io.ahamdy.jobforce.store

import fs2.Task
import io.ahamdy.jobforce.common.Time
import io.ahamdy.jobforce.domain._

class DummyNodeStore(time: Time) extends NodeStore {
  override def getAllNodes: Task[List[JobNode]] = Task.now(List(
    JobNode(NodeId("test-node-1"), NodeGroup("test-group-1"), time.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0")),
    JobNode(NodeId("test-node-2"), NodeGroup("test-group-1"), time.unsafeNow(), NodeActive(true), NodeVersion("1.0.0")),
    JobNode(NodeId("test-node-3"), NodeGroup("test-group-2"), time.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0")) // other group
  ))
}
