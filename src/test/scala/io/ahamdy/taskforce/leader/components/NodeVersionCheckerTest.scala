package io.ahamdy.taskforce.leader.components

import java.time.ZonedDateTime

import io.ahamdy.taskforce.domain._
import io.ahamdy.taskforce.testing.StandardSpec

class NodeVersionCheckerTest extends StandardSpec {
  val startTime: ZonedDateTime = ZonedDateTime.now()
  def createJobNode(nodeId: String, nodeVersion: String): JobNode =
    JobNode(NodeId(nodeId), NodeGroup("test-group-1"), startTime, NodeActive(true), NodeVersion(nodeVersion))

  "NodeVersionChecker" should {
    "checkVersion" in {
      NodeVersionChecker.checkVersion(JobVersionRule.IGNORE,
        createJobNode("node-1", "1.2.3")) must beTrue
      NodeVersionChecker.checkVersion(JobVersionRule.IGNORE,
        createJobNode("node-1", "2.2.3")) must beTrue

      NodeVersionChecker.checkVersion(JobVersionRule(VersionRuleDirective.AtLeast, NodeVersion("2.0.0")),
        createJobNode("node-1", "1.2.3")) must beFalse
      NodeVersionChecker.checkVersion(JobVersionRule(VersionRuleDirective.AtLeast, NodeVersion("2.0.0")),
        createJobNode("node-1", "2.2.3")) must beTrue

      NodeVersionChecker.checkVersion(JobVersionRule(VersionRuleDirective.AtMost, NodeVersion("2.0.0")),
        createJobNode("node-1", "1.2.3")) must beTrue
      NodeVersionChecker.checkVersion(JobVersionRule(VersionRuleDirective.AtMost, NodeVersion("2.0.0")),
        createJobNode("node-1", "2.2.3")) must beFalse

      NodeVersionChecker.checkVersion(JobVersionRule(VersionRuleDirective.Exactly, NodeVersion("2.0.0")),
        createJobNode("node-1", "1.2.3")) must beFalse
      NodeVersionChecker.checkVersion(JobVersionRule(VersionRuleDirective.Exactly, NodeVersion("2.0.0")),
        createJobNode("node-1", "2.2.3")) must beFalse
      NodeVersionChecker.checkVersion(JobVersionRule(VersionRuleDirective.Exactly, NodeVersion("2.0.0")),
        createJobNode("node-1", "2.0.0")) must beTrue
    }
  }
}
