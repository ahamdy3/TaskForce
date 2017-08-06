package io.ahamdy.jobforce.leader.components

import java.time.ZonedDateTime

import io.ahamdy.jobforce.domain._
import io.ahamdy.jobforce.testing.StandardSpec

class NodeLoadBalancerTest extends StandardSpec {

  val now: ZonedDateTime = ZonedDateTime.now()

  def createJobNode(nodeId: String, startTime: ZonedDateTime = now, nodeVersion: String = "1.0.0"): JobNode =
    JobNode(NodeId(nodeId), NodeGroup("test-group-1"), startTime, NodeActive(true), NodeVersion(nodeVersion))

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
    "combineNodeLoads" should {
      "combine two NodeLoad lists while summing the common loads together" in {
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
    }

    "leastLoadedNode" should {
      "return the least loaded node according to position" in {
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

        NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes, JobVersionRule.IGNORE, NodeId("node-1"),
          leaderAlsoWorker = true, position = 0) must beSome(NodeLoad(createJobNode("node-3"), 1))

        NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes, JobVersionRule.IGNORE, NodeId("node-1"),
          leaderAlsoWorker = true, position = 1) must beSome(NodeLoad(createJobNode("node-2"), 2))

        NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes, JobVersionRule.IGNORE, NodeId("node-1"),
          leaderAlsoWorker = true, position = 2) must beSome(NodeLoad(createJobNode("node-1"), 3))

        NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes, JobVersionRule.IGNORE, NodeId("node-1"),
          leaderAlsoWorker = true, position = 3) must beNone
      }

      "return the least loaded node ordered by startTime if they have the same weight" in {
        val activeNodes = List(
          createJobNode("node-1", now.minusSeconds(2)),
          createJobNode("node-2", now.minusSeconds(5)),
          createJobNode("node-3", now.minusSeconds(1)),
        )

        val runningJobs = List(
          createRunningJob("job-id-1", "node-1", 3),
          createRunningJob("job-id-1", "node-2", 3),
          createRunningJob("job-id-1", "node-3", 3),
        )

        NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes, JobVersionRule.IGNORE, NodeId("node-2"),
          leaderAlsoWorker = true, position = 0) must beSome(NodeLoad(createJobNode("node-2", now.minusSeconds(5)), 3))
      }

      "return the least loaded node ordered by nodeId if they have the same weight and startTime" in {
        val activeNodes = List(
          createJobNode("node-1"),
          createJobNode("node-2"),
          createJobNode("node-3"),
        )

        val runningJobs = List(
          createRunningJob("job-id-1", "node-1", 3),
          createRunningJob("job-id-1", "node-2", 3),
          createRunningJob("job-id-1", "node-3", 3),
        )

        NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes, JobVersionRule.IGNORE, NodeId("node-2"),
          leaderAlsoWorker = true, position = 0) must beSome(NodeLoad(createJobNode("node-1"), 3))
      }

      "not include leader if leaderAlsoWorker = false" in {
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

        NodeLoadBalancer.leastLoadedNode(runningJobs, allNodes, JobVersionRule.IGNORE, NodeId("node-1"),
          leaderAlsoWorker = true, position = 0) must beSome(NodeLoad(createJobNode("node-1"), 1))

        NodeLoadBalancer.leastLoadedNode(runningJobs, allNodes, JobVersionRule.IGNORE, NodeId("node-1"),
          leaderAlsoWorker = false, position = 0) must beSome(NodeLoad(createJobNode("node-2"), 2))
      }

      "not include a non existing node in activeNodesList even if they are running jobs" in {
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

        NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes, JobVersionRule.IGNORE, NodeId("node-1"),
          leaderAlsoWorker = true, position = 0) must beSome(NodeLoad(createJobNode("node-3"), 1))

        NodeLoadBalancer.leastLoadedNode(runningJobs, updatedActiveNodes, JobVersionRule.IGNORE, NodeId("node-1"),
          leaderAlsoWorker = false, position = 0) must beSome(NodeLoad(createJobNode("node-2"), 2))

        NodeLoadBalancer.leastLoadedNode(runningJobs, List.empty, JobVersionRule.IGNORE, NodeId("node-1"),
          leaderAlsoWorker = false, position = 0) must beNone
      }

      "return the least loaded node with required node version or None" in {
        val activeNodes = List(
          createJobNode("node-1", nodeVersion = "1.0.1"),
          createJobNode("node-2", nodeVersion = "1.0.2"),
          createJobNode("node-3", nodeVersion = "1.0.3"),
          createJobNode("node-4", nodeVersion = "1.0.4"),
          createJobNode("node-5", nodeVersion = "1.0.5"),
        )

        val runningJobs = List(
          createRunningJob("job-id-1", "node-1", 1),
          createRunningJob("job-id-1", "node-2", 2),
          createRunningJob("job-id-1", "node-3", 3),
          createRunningJob("job-id-1", "node-4", 2),
          createRunningJob("job-id-1", "node-5", 1),
        )

        NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes, JobVersionRule(VersionRuleDirective.Exactly, NodeVersion("1.0.3")),
          NodeId("node-1"), leaderAlsoWorker = true, position = 0) must beSome(NodeLoad(createJobNode("node-3", nodeVersion = "1.0.3"), 3))

        NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes, JobVersionRule(VersionRuleDirective.AtLeast, NodeVersion("1.0.3")),
          NodeId("node-1"), leaderAlsoWorker = true, position = 0) must beSome(NodeLoad(createJobNode("node-5", nodeVersion = "1.0.5"), 1))

        NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes, JobVersionRule(VersionRuleDirective.AtMost, NodeVersion("1.0.3")),
          NodeId("node-1"), leaderAlsoWorker = true, position = 0) must beSome(NodeLoad(createJobNode("node-1", nodeVersion = "1.0.1"), 1))

        NodeLoadBalancer.leastLoadedNode(runningJobs, activeNodes, JobVersionRule(VersionRuleDirective.AtLeast, NodeVersion("2.0.0")),
          NodeId("node-1"), leaderAlsoWorker = true, position = 0) must beNone
      }
    }

    "canNodeHandle" should {
      "return true only if current node load + job load still less than or equal maxLoadPerNode" in {
        NodeLoadBalancer.canNodeHandle(nodeCurrentWeight = 10, jobWeight = 80, maxWeightPerNode = 100) must beTrue
        NodeLoadBalancer.canNodeHandle(nodeCurrentWeight = 10, jobWeight = 90, maxWeightPerNode = 100) must beTrue
        NodeLoadBalancer.canNodeHandle(nodeCurrentWeight = 10, jobWeight = 100, maxWeightPerNode = 100) must beFalse
      }

      "treat jobs having weight larger than maxLoadPerNode as a job with wieght = maxLoadPerNode" in {
        NodeLoadBalancer.canNodeHandle(nodeCurrentWeight = 0, jobWeight = 100, maxWeightPerNode = 100) must beTrue
        NodeLoadBalancer.canNodeHandle(nodeCurrentWeight = 0, jobWeight = 150, maxWeightPerNode = 100) must beTrue

        NodeLoadBalancer.canNodeHandle(nodeCurrentWeight = 10, jobWeight = 100, maxWeightPerNode = 100) must beFalse
        NodeLoadBalancer.canNodeHandle(nodeCurrentWeight = 10, jobWeight = 150, maxWeightPerNode = 100) must beFalse
      }
    }
  }
}
