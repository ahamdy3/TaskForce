package io.ahamdy.jobforce.testing

import org.specs2.matcher.{TerminationMatchers, ThrownExpectations}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

trait StandardSpec extends Specification
  with ScalaCheck
  with TaskMatchers
  with TerminationMatchers
  with ThrownExpectations
