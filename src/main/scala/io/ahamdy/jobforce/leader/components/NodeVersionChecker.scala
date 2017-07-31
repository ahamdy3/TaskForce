package io.ahamdy.jobforce.leader.components

import io.ahamdy.jobforce.domain.{JobVersionRule, NodeLoad, VersionRuleDirective}

object NodeVersionChecker {
  def checkVersion(versionRule: JobVersionRule, nodeLoad: NodeLoad): Boolean =
    versionRule match {
      case JobVersionRule(VersionRuleDirective.AnyVersion, _) => true
      case JobVersionRule(VersionRuleDirective.Exactly, jobVersion) => nodeLoad.node.version == jobVersion
      case JobVersionRule(VersionRuleDirective.AtLeast, jobVersion) => nodeLoad.node.version.value >= jobVersion.value
      case JobVersionRule(VersionRuleDirective.AtMost, jobVersion) => nodeLoad.node.version.value <= jobVersion.value
    }
}
