package io.ahamdy.jobforce.config

import org.specs2.Specification


class JobForceConfigTest extends Specification { def is = s2"""
  Environment configs can be loaded:
    application-test:      ${testEnv("application-test.conf")}
    """


  def testEnv(envName: String) = JobForceConfig.load(envName) must beRight
}
