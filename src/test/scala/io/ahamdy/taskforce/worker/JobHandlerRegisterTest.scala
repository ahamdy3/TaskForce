package io.ahamdy.taskforce.worker

import io.ahamdy.taskforce.domain.JobType
import io.ahamdy.taskforce.testing.StandardSpec

class JobHandlerRegisterTest extends StandardSpec {

  val jobHandler = new DummyJobHandler(JobType("test-type"))

  "JobHandlerRegister" should {
    "registerHandler must register handler only if there's no handler already registered" in {
      val jobHandlerRegister = new JobHandlerRegisterImpl

      jobHandlerRegister.registerHandler(jobHandler) must beRight
      jobHandlerRegister.jobHandlers.containsValue(jobHandler) must beTrue

      jobHandlerRegister.registerHandler(jobHandler) must beLeft
    }

    "getJobHandler must getjob handler by type if exist" in {
      val jobHandlerRegister = new JobHandlerRegisterImpl

      jobHandlerRegister.registerHandler(jobHandler) must beRight

      jobHandlerRegister.getJobHandler(jobHandler.jobType) must beSome
      jobHandlerRegister.getJobHandler(JobType("test-type-2")) must beNone
    }
  }
}
