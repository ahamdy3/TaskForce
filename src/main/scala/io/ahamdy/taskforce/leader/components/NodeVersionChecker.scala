package io.ahamdy.taskforce.leader.components

import io.ahamdy.taskforce.domain.{JobNode, JobVersionRule, VersionRuleDirective}

object NodeVersionChecker {
  def checkVersion(versionRule: JobVersionRule, node: JobNode): Boolean =
    versionRule match {
      case JobVersionRule(VersionRuleDirective.AnyVersion, _) => true
      case JobVersionRule(VersionRuleDirective.Exactly, jobVersion) => node.version == jobVersion
      case JobVersionRule(VersionRuleDirective.AtLeast, jobVersion) => node.version.value >= jobVersion.value
      case JobVersionRule(VersionRuleDirective.AtMost, jobVersion) => node.version.value <= jobVersion.value
    }
}
