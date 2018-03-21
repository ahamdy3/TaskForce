package io.ahamdy.taskforce.worker

import java.time.ZonedDateTime

import cats.effect.IO
import io.ahamdy.taskforce.api.{DummyNodeInfoProvider, Worker}
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
    data = Map.empty,
    attempts = JobAttempts(0, JobMaxAttempts(3)),
    priority = JobPriority(2),
    queuingTime = time.unsafeNow(),
    parentJob = None,
    versionRule = JobVersionRule.IGNORE
  )

  val runningJob = queuedJob.toRunningJobAndIncAttempts(NodeId("node-id-1"), NodeGroup("node-group-1"), time.unsafeNow())

  val config = WorkerDutiesConfig()

  val jobStore = new DummyJobStore(time)
  val nodeInfoProvider = new DummyNodeInfoProvider("test-node-1", "test-group-1")
  val nodeStore = new DummyNodeStore(time)
  val jobHandlerRegister = new JobHandlerRegisterImpl
  // val jobsStrategy: Strategy = Strategy.fromFixedDaemonPool(3)

  val workerDuties = new WorkerDutiesImpl(config, jobStore, nodeInfoProvider, nodeStore, jobHandlerRegister, time)

  "WorkerDutiesTest" should {
    "queueJob must queue provided job into jobStore" in {
      jobStore.reset()

      workerDuties.queueJob(queuedJob) must beSucceedingIO
      jobStore.queuedJobStore.containsValue(queuedJob) must beTrue
    }

    "requeueJob must queue provided runningJob only if it hasn't exceeded maxAttempts or else marked as a failed finished job" in {
      jobStore.reset()
      jobStore.runningJobStore.put(runningJob.lock, runningJob)

      workerDuties.requeueJob(runningJob) must beSucceedingIO
      jobStore.queuedJobStore.containsKey(runningJob.id) must beTrue
      jobStore.runningJobStore.containsKey(runningJob.lock) must beFalse
      jobStore.finishedJobStore.map(_.id).contains(runningJob.id) must beFalse

      jobStore.reset()
      jobStore.runningJobStore.put(runningJob.lock, runningJob)
      val maxTriedRunningJob = runningJob.copy(attempts = runningJob.attempts.copy(attempts = 3))
      workerDuties.requeueJob(maxTriedRunningJob) must beSucceedingIO
      jobStore.queuedJobStore.containsKey(runningJob.id) must beFalse
      jobStore.runningJobStore.containsKey(runningJob.lock) must beFalse
      jobStore.finishedJobStore.map(_.id).contains(runningJob.id) must beTrue
    }

    "finishJob" in {
      jobStore.reset()
      jobStore.runningJobStore.put(runningJob.lock, runningJob)

      workerDuties.finishJob(runningJob.toFinishedJob(time.unsafeNow(), JobResult.Success)) must beSucceedingIO
      jobStore.runningJobStore.containsKey(runningJob.lock) must beFalse
      jobStore.finishedJobStore.map(_.id).contains(runningJob.id) must beTrue
    }

    "runValidation must run validations on provided data and return a successful task with valid data, otherwise return failed task" in {
      val validData = Map("VALID" -> "VALID_DATA")
      val validationFunction = { data: Map[String, String] =>
        if(data.contains("VALID"))
          IO.pure(data)
        else
          IO.raiseError(new Exception("fake INVALID test exception"))
      }

      val jobHandler = new DummyJobHandler(runningJob.jobType, validationFunction)

      val jobWithValidData = runningJob.copy(data = validData)
      workerDuties.runValidation(jobHandler, jobWithValidData) must beSucceedingIO(validData)

      val invalidData = Map("INVALID" -> "INVALID_DATA")
      val jobWithInvalidData = runningJob.copy(data = invalidData)
      workerDuties.runValidation(jobHandler, jobWithInvalidData) must beFailingIO(new Exception("fake INVALID test exception"))
    }

    "runAssignedJob" should {
      "run assigned jobs only if they are not already running" in {
        jobStore.reset()
        jobHandlerRegister.reset()
        val jobHandler = new DummyJobHandler(runningJob.jobType)
        jobHandlerRegister.registerHandler(jobHandler)

        workerDuties.runAssignedJob(runningJob) must beSucceedingIO

        jobHandler.totalRuns.get mustEqual 1
        jobHandler.successfulRuns.get mustEqual 1
        jobHandler.failedRuns.get mustEqual 0
      }

      "run assigned jobs and use provided error handler to handle error" in {
        jobStore.reset()
        jobHandlerRegister.reset()
        val jobHandler = new DummyJobHandler(runningJob.jobType)
        jobHandlerRegister.registerHandler(jobHandler)

        val failingWithRetryRunningJob = runningJob.copy(data = Map(jobHandler.FAIL_WITH_RETRY -> "something"))

        workerDuties.runAssignedJob(failingWithRetryRunningJob) must beSucceedingIO

        jobStore.queuedJobStore.containsKey(runningJob.id) must beTrue
        jobStore.queuedJobStore.get(runningJob.id).attempts.attempts mustEqual 1
        jobHandler.successfulRuns.get mustEqual 0
      }
    }
  }
}
