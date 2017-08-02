package io.ahamdy.jobforce.leader.components

import java.time.ZonedDateTime

import io.ahamdy.jobforce.domain._
import io.ahamdy.jobforce.testing.StandardSpec

class NodeLoadBalancerTest extends StandardSpec {

  val now: ZonedDateTime = ZonedDateTime.now()

  def createJobNode(nodeId: String): JobNode =
    JobNode(NodeId(nodeId), NodeGroup("test-group-1"), now, NodeActive(true), NodeVersion("1.0.0"))

  def createRunningJob(jobId: String, nodeId: String, weight: Int) = RunningJob(
    JobId(jobId),
    NodeId(nodeId),
    JobLock("test-lock-1"),
    JobType("test-type-1"),
    JobWeight(weight),
    Map.empty,
    JobAttempts(1, JobMaxAttempts(5)),
    JobPriority(1),
    now,
    now.plusMinutes(1),
    None,
    JobVersionRule.IGNORE
  )

  "NodeLoadBalancer" should {
    "combineNodeLoads should combine two NodeLoad lists while summing the common loads together" in {
      val list1 = List(
        NodeLoad(createJobNode("node-1"), 1),
        NodeLoad(createJobNode("node-2"), 1),
        NodeLoad(createJobNode("node-3"), 1),
        NodeLoad(createJobNode("node-4"), 1),
      )

      val list2 = List(
        NodeLoad(createJobNode("node-1"), 2),
        NodeLoad(createJobNode("node-2"), 3),
        NodeLoad(createJobNode("node-3"), 4),
        NodeLoad(createJobNode("node-5"), 5),
      )

      val expectedList = List(
        NodeLoad(createJobNode("node-1"), 3),
        NodeLoad(createJobNode("node-2"), 4),
        NodeLoad(createJobNode("node-3"), 5),
        NodeLoad(createJobNode("node-4"), 1),
        NodeLoad(createJobNode("node-5"), 5),
      )

      NodeLoadBalancer.combineNodeLoads(list1, list2) must containTheSameElementsAs(expectedList)
    }

    "leastLoadedNode must return the least loaded node" in {
      val activeNodes = List(
        createJobNode("node-1"),
        createJobNode("node-2"),
        createJobNode("node-3"),
      )

      val runningJobs = List(
        createRunningJob("job-id-1", "node-1", 3),
        createRunningJob("job-id-1", "node-2", 2),
        createRunningJob("job-id-1", "node-3", 1),
      )

      NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes,
        JobVersionRule.IGNORE, NodeId("node-1"), leaderAlsoWorker = true) must beSome(NodeLoad(createJobNode("node-3"), 1))
    }

    "leastLoadedNode must not include leader if leaderAlsoWorker = false" in {
      val allNodes = List(
        createJobNode("node-1"),
        createJobNode("node-2"),
        createJobNode("node-3"),
      )

      val runningJobs = List(
        createRunningJob("job-id-1", "node-1", 1),
        createRunningJob("job-id-1", "node-2", 2),
        createRunningJob("job-id-1", "node-3", 3),
      )

      NodeLoadBalancer.leastLoadedNode(runningJobs, allNodes,
        JobVersionRule.IGNORE, NodeId("node-1"), leaderAlsoWorker = true) must beSome(NodeLoad(createJobNode("node-1"), 1))

      NodeLoadBalancer.leastLoadedNode(runningJobs, allNodes,
        JobVersionRule.IGNORE, NodeId("node-1"), leaderAlsoWorker = false) must beSome(NodeLoad(createJobNode("node-2"), 2))
    }

    "leastLoadedNode must not include a non existing node in activeNodesList even if they are running jobs" in {
      val activeNodes = List(
        createJobNode("node-1"),
        createJobNode("node-2"),
        createJobNode("node-3"),
      )

      val updatedActiveNodes = List(
        createJobNode("node-1"),
        createJobNode("node-2"),
      )

      val runningJobs = List(
        createRunningJob("job-id-1", "node-1", 3),
        createRunningJob("job-id-1", "node-2", 2),
        createRunningJob("job-id-1", "node-3", 1),
      )

      NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes,
        JobVersionRule.IGNORE, NodeId("node-1"), leaderAlsoWorker = true) must beSome(NodeLoad(createJobNode("node-3"), 1))

      NodeLoadBalancer.leastLoadedNode(runningJobs, updatedActiveNodes,
        JobVersionRule.IGNORE, NodeId("node-1"), leaderAlsoWorker = false) must beSome(NodeLoad(createJobNode("node-2"), 2))
    }

  }
}
