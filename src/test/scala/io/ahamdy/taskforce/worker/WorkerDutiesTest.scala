package io.ahamdy.taskforce.worker

import java.time.ZonedDateTime

import fs2.Strategy
import io.ahamdy.taskforce.api.DummyNodeInfoProvider
import io.ahamdy.taskforce.common.DummyTime
import io.ahamdy.taskforce.domain._
import io.ahamdy.taskforce.store.{DummyJobStore, DummyNodeStore}
import io.ahamdy.taskforce.testing.StandardSpec

class WorkerDutiesTest extends StandardSpec {

  sequential

  val time = new DummyTime(ZonedDateTime.now())

  val queuedJob = QueuedJob(
    JobId("job-id-1"),
    JobLock("test-lock-1"),
    JobType("test-type-1"),
    JobWeight(5),
    Map.empty,
    JobAttempts(0, JobMaxAttempts(3)),
    JobPriority(2),
    time.unsafeNow(),
    None,
    JobVersionRule.IGNORE
  )

  val config = WorkerDutiesConfig()

  val jobStore = new DummyJobStore(time)
  val nodeInfoProvider = new DummyNodeInfoProvider("test-node-1", "test-group-1")
  val nodeStore = new DummyNodeStore(time)
  val jobHandlerRegister = new JobHandlerRegisterImpl
  val jobsStrategy: Strategy = Strategy.fromFixedDaemonPool(3)

  val workerDuties = new WorkerDutiesImpl(config, jobStore, nodeInfoProvider, nodeStore, jobHandlerRegister, time, jobsStrategy)

  "WorkerDutiesTest" should {
    "queueJob must queue provided job into jobStore" in {
      jobStore.reset()

      workerDuties.queueJob(queuedJob) must beSucceedingTask
      jobStore.queuedJobStore.containsValue(queuedJob) must beTrue
    }

    "requeueJob must queue provided job only if it hasn't exceeded maxAttempts or else marked as a failed finished job" in {
      jobStore.reset()
      ok
    }

    "requeueJob$default$2" in {
      ok
    }

    "finishJob" in {
      ok
    }

    "runValidation" in {
      ok
    }

    "runAssignedJobs" in {
      ok
    }
  }
}
