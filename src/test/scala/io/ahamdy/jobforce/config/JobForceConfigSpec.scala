package io.ahamdy.jobforce.config

import io.ahamdy.jobforce.testing.StandardSpec

class JobForceConfigSpec extends StandardSpec { def is = s2"""
  Environment configs can be loaded:
    application-test:      ${testEnv("application-test.conf")}
    """


  def testEnv(envName: String) = JobForceConfig.load(envName) must beRight
}
