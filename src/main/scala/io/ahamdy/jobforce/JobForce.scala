package io.ahamdy.jobforce

import io.ahamdy.jobforce.config.JobForceConfig

/**
  * Created by ahamdy on 7/16/17.
  */
class JobForce {

  def build = {
    val configName = Option(System.getenv("JF_CONFIG")).getOrElse("application.conf")
    val config = JobForceConfig.load(configName)
  }

}
