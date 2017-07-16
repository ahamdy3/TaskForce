package io.ahamdy.jobforce.config

import com.ccadllc.cedi.config.{ConfigErrors, ConfigParser}
import com.typesafe.config.ConfigFactory

case class JobForceConfig(minimumNodes: Int)

object JobForceConfig {

  def load(name: String): Either[ConfigErrors, JobForceConfig] = {
    val config = ConfigFactory.load(name)
    ConfigParser.derived[JobForceConfig].parse(config)
  }
}
