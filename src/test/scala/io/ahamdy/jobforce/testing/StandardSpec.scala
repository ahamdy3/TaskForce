package io.ahamdy.jobforce.testing

import org.specs2.matcher.{TerminationMatchers, ThrownExpectations}
import org.specs2.{ScalaCheck, Specification}

trait StandardSpec extends Specification
  with ScalaCheck
  with TerminationMatchers
  with ThrownExpectations
