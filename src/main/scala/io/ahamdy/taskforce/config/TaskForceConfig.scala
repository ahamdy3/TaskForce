package io.ahamdy.taskforce.config

import com.ccadllc.cedi.config.{ConfigErrors, ConfigParser}
import com.typesafe.config.ConfigFactory

case class TaskForceConfig(minimumNodes: Int)

object TaskForceConfig {

  def load(name: String): Either[ConfigErrors, TaskForceConfig] = {
    val config = ConfigFactory.load(name)
    ConfigParser.derived[TaskForceConfig].parse(config)
  }
}
