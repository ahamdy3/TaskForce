package io.ahamdy.taskforce

import io.ahamdy.taskforce.config.TaskForceConfig

class TaskForce {

  def build = {
    val configName = Option(System.getenv("TF_CONFIG")).getOrElse("application.conf")
    val config = TaskForceConfig.load(configName)
  }

}
