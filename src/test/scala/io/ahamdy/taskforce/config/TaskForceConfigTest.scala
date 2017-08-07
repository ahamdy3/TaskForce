package io.ahamdy.taskforce.config

import org.specs2.Specification


class TaskForceConfigTest extends Specification { def is = s2"""
  Environment configs can be loaded:
    application-test:      ${testEnv("application-test.conf")}
    """


  def testEnv(envName: String) = TaskForceConfig.load(envName) must beRight
}
