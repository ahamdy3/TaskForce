package io.ahamdy.jobforce.leader

import java.time.{ZoneId, ZonedDateTime}
import java.util.concurrent.ConcurrentHashMap

import com.cronutils.model.CronType
import fs2.Task
import fs2.interop.cats._
import cats.syntax.flatMap._
import io.ahamdy.jobforce.common.{DummyTime, Time}
import io.ahamdy.jobforce.domain._
import io.ahamdy.jobforce.scheduling.{CronLine, JobsScheduleProvider}
import io.ahamdy.jobforce.shared.NodeInfoProvider
import io.ahamdy.jobforce.store.{JobsStore, NodeStore}
import io.ahamdy.jobforce.testing.StandardSpec

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import io.ahamdy.jobforce.testing.syntax.either._


class LeaderDutiesImplTest extends StandardSpec{

  sequential

  val scheduledJob = ScheduledJob(
    JobId("job-id-1"),
    JobLock("test-lock-1"),
    JobType("test-type-1"),
    JobWeight(5),
    Map.empty,
    JobSchedule(CronLine.parse("0 * * * * ?", CronType.QUARTZ, ZoneId.of("UTC")).getRight, 2.minute),
    JobMaxAttempts(5),
    JobPriority(2)
  )

  val dummyTime = new DummyTime(ZonedDateTime.now())

  val node1InfoProvider = new NodeInfoProvider {
    override def nodeGroup: NodeGroup = NodeGroup("test-group-1")
    override def nodeId: NodeId = NodeId("test-node-1")
  }

  val node2InfoProvider = new NodeInfoProvider {
    override def nodeGroup: NodeGroup = NodeGroup("test-group-2")
    override def nodeId: NodeId = NodeId("test-node-1")
  }

  val jobsScheduleProvider = new JobsScheduleProvider {
    val scheduledJobs: ListBuffer[ScheduledJob] = mutable.ListBuffer.empty
    override def getJobsSchedule: Task[List[ScheduledJob]] =
      Task.delay(scheduledJobs.toList)

    def reset(): Unit = scheduledJobs.clear()
  }

  val nodeStore = new NodeStore {
    override def getAllNodes: Task[List[JobNode]] = Task.now(List(
      JobNode(NodeId("test-node-1"), NodeGroup("test-group-1"), dummyTime.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0")),
      JobNode(NodeId("test-node-2"), NodeGroup("test-group-1"), dummyTime.unsafeNow(), NodeActive(true), NodeVersion("1.0.0")),
      JobNode(NodeId("test-node-3"), NodeGroup("test-group-2"), dummyTime.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0"))
    ))
  }

  val jobsStore = new JobsStore {
    val queuedJobStore = new ConcurrentHashMap[JobId, QueuedJob]()
    val runningJobStore = new ConcurrentHashMap[JobLock, RunningJob]()
    val finishedJobStore = new mutable.ArrayBuffer[FinishedJob]()

    override def getJobLastRunTime(id: JobId): Task[Option[ZonedDateTime]] =
      if(queuedJobStore.containsKey(id) || runningJobStore.containsKey(id))
        dummyTime.now.map(Some(_))
      else
        finishedJobStore.toList.map(_.finishTime) match {
          case Nil => Task.now(None)
          case times if times.length > 1 => Task.delay(Some(times.maxBy(_.getNano)))
        }

    override def getQueuedJobsOrderedByPriority: Task[List[QueuedJob]] =
      Task.delay(queuedJobStore.values().asScala.toList.sortBy(_.priority.value))

    override def moveQueuedJobToRunningJob(runningJob: RunningJob): Task[Unit] =
      if(Option(runningJobStore.putIfAbsent(runningJob.lock, runningJob)).isEmpty)
        Task.delay(queuedJobStore.remove(runningJob.id)).map(_ => ())
      else
        Task.fail(new Exception("failed to move queued job to running job"))

    override def getRunningJobs: Task[List[RunningJob]] =
      Task.delay(runningJobStore.values().asScala.toList)

    override def createQueuedJob(queuedJob: QueuedJob): Task[Boolean] =
      Task.delay(Option(queuedJobStore.putIfAbsent(queuedJob.id, queuedJob)).isEmpty)

    override def getFinishedJobs: Task[List[FinishedJob]] =
      Task.delay(finishedJobStore.toList)

    override def moveRunningJobToQueuedJob(queuedJob: QueuedJob): Task[Unit] =
      Task.delay(runningJobStore.remove(queuedJob.id)) >>
      Task.delay{
        if(Option(queuedJobStore.putIfAbsent(queuedJob.id, queuedJob)).isEmpty)
          ()
        else
          new Exception("failed to move queued job to running job")
      }


    override def moveRunningJobToFinishedJob(finishedJob: FinishedJob): Task[Unit] =
      Task.delay(runningJobStore.remove(finishedJob.id)) >>
        Task.delay(finishedJobStore.append(finishedJob))

    override def getRunningJobsByNodeId(nodeId: NodeId): Task[List[RunningJob]] =
      getRunningJobs.map(_.filter(_.nodeId == nodeId))

    def reset(): Unit = {
      queuedJobStore.clear()
      runningJobStore.clear()
      finishedJobStore.clear()
    }

    def isEmpty: Boolean = queuedJobStore.isEmpty && runningJobStore.isEmpty && finishedJobStore.isEmpty
  }

  def createNewLeader(config: JobForceLeaderConfig = config,
                      nodeInfoProvider: NodeInfoProvider = node1InfoProvider,
                      jobsScheduleProvider: JobsScheduleProvider = jobsScheduleProvider,
                      nodeStore: NodeStore = nodeStore,
                      jobsStore: JobsStore = jobsStore,
                      time: Time = dummyTime) =
    new LeaderDutiesImpl(
      config,
      nodeInfoProvider,
      jobsScheduleProvider,
      nodeStore,
      jobsStore,
      time
    )

  val config = JobForceLeaderConfig(
    minActiveNodes = 2,
    maxWeightPerNode = 100,
    youngestLeaderAge = 10.second,
    leaderAlsoWorker = true)

  "LeaderDutiesImpl.electClusterLeader" should  {
    "elect oldest node as leader" in {
      jobsStore.reset()
      val leader = createNewLeader()
      val nonLeader = createNewLeader(nodeInfoProvider = node2InfoProvider)


      leader.isLeader must beFalse
      nonLeader.isLeader must beFalse

      leader.electClusterLeader must beSucceedingTask
      nonLeader.electClusterLeader must beSucceedingTask

      leader.isLeader must beTrue
      nonLeader.isLeader must beFalse
    }

    "refresh ScheduledJobs, QueuedJobs and runningJobs in leader cache when leader is elected" in {
      jobsStore.reset()

      jobsScheduleProvider.scheduledJobs.append(scheduledJob)

      val queuedJob = scheduledJob.copy(id = JobId("job-id-2")).toQueuedJob(dummyTime.unsafeNow())
      jobsStore.queuedJobStore.put(queuedJob.id, queuedJob)

      val runningJob = queuedJob.copy(id = JobId("job-id-3"))
        .toRunningJobAndIncAttempts(NodeId("test-node-2"), dummyTime.unsafeNow())
      jobsStore.runningJobStore.put(runningJob.lock, runningJob)

      val leader = createNewLeader()
      leader.electClusterLeader must beSucceedingTask

      leader.scheduledJobs.get().toSet mustEqual Set(scheduledJob)
      leader.queuedJobs.values().asScala.toSet mustEqual Set(queuedJob)
      leader.runningJobs.values().asScala.toSet mustEqual Set(runningJob)
    }

    "never elect a leader if the oldest node in the group is younger than configured youngestLeaderAge" in {
      jobsStore.reset()
      val modifiedNodeStore = new NodeStore {
        override def getAllNodes: Task[List[JobNode]] = Task.now(List(
          JobNode(NodeId("test-node-1"), NodeGroup("test-group-1"), dummyTime.unsafeNow().minusSeconds(2), NodeActive(true), NodeVersion("1.0.0")),
          JobNode(NodeId("test-node-2"), NodeGroup("test-group-1"), dummyTime.unsafeNow().minusSeconds(1), NodeActive(true), NodeVersion("1.0.0")),
          JobNode(NodeId("test-node-3"), NodeGroup("test-group-2"), dummyTime.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0"))
        ))
      }

      val leader = createNewLeader(nodeStore = modifiedNodeStore)
      val nonLeader = createNewLeader(nodeStore = modifiedNodeStore, nodeInfoProvider = node2InfoProvider)

      leader.isLeader must beFalse
      nonLeader.isLeader must beFalse

      leader.electClusterLeader must beSucceedingTask
      nonLeader.electClusterLeader must beSucceedingTask

      leader.isLeader must beFalse
      nonLeader.isLeader must beFalse
    }
  }

  "LeaderDutiesImpl.refreshQueuedJobs" should {
    "refresh QueuedJobs in leader cache and do nothing if not a leader" in {
      jobsStore.reset()

      val leader = createNewLeader()
      val nonLeader = createNewLeader(nodeInfoProvider = node2InfoProvider)

      leader.electClusterLeader must beSucceedingTask

      leader.queuedJobs.values().asScala must beEmpty
      nonLeader.queuedJobs.values().asScala must beEmpty

      val queuedJob = scheduledJob.toQueuedJob(dummyTime.unsafeNow())
      jobsStore.queuedJobStore.put(queuedJob.id, queuedJob)

      leader.refreshQueuedJobs must beSucceedingTask
      nonLeader.refreshQueuedJobs must beSucceedingTask

      leader.queuedJobs.values().asScala.toSet mustEqual Set(queuedJob)
      nonLeader.queuedJobs.values().asScala must beEmpty
    }
  }

  "LeaderDutiesImpl.refreshJobsSchedule" should {
    "refresh ScheduledJobs in leader cache only if leader or leaderIgnore flag is true" in {
      jobsStore.reset()
      jobsScheduleProvider.reset()

      val leader = createNewLeader()
      val nonLeader = createNewLeader(nodeInfoProvider = node2InfoProvider)

      leader.electClusterLeader must beSucceedingTask

      jobsScheduleProvider.scheduledJobs.append(scheduledJob)

      leader.refreshJobsSchedule() must beSucceedingTask
      nonLeader.refreshJobsSchedule() must beSucceedingTask

      leader.scheduledJobs.get.toSet mustEqual Set(scheduledJob)
      nonLeader.scheduledJobs.get.toSet must beEmpty

      val scheduledJob2 = scheduledJob.copy(id = JobId("job-id-2"))
      jobsScheduleProvider.scheduledJobs.append(scheduledJob2)

      leader.refreshJobsSchedule() must beSucceedingTask
      nonLeader.refreshJobsSchedule() must beSucceedingTask

      leader.scheduledJobs.get.toSet mustEqual Set(scheduledJob, scheduledJob2)
      nonLeader.scheduledJobs.get.toSet must beEmpty

      nonLeader.refreshJobsSchedule(ignoreLeader = true) must beSucceedingTask
      leader.scheduledJobs.get.toSet mustEqual Set(scheduledJob, scheduledJob2)
    }
  }

  "LeaderDutiesImpl.queueScheduledJobs" should {
    "queue due scheduled jobs only if leader and if the job is not already queued or running" in {
      jobsStore.reset()
      jobsScheduleProvider.reset()

      val leader = createNewLeader()

      leader.electClusterLeader must beSucceedingTask

      jobsStore.isEmpty must beTrue
      jobsScheduleProvider.scheduledJobs must beEmpty
      leader.queuedJobs.isEmpty must beTrue

      leader.queueScheduledJobs must beSucceedingTask

      leader.queuedJobs.isEmpty must beTrue

      jobsScheduleProvider.scheduledJobs.append(scheduledJob)
      leader.refreshJobsSchedule() must beSucceedingTask

      leader.queueScheduledJobs must beSucceedingTask

      leader.queuedJobs.values().asScala.toList mustEqual List(scheduledJob.toQueuedJob(dummyTime.unsafeNow()))
    }
  }

  "LeaderDutiesImpl.assignQueuedJobs" should {
    "assign queued jobs to active nodes with respect to load balancing and version requirements" in {

      val scheduledJob1 = scheduledJob.copy(id = JobId("test-job1"), lock = JobLock("lock-1"),
        weight = JobWeight(100), priority = JobPriority(2))
      val scheduledJob2 = scheduledJob.copy(id = JobId("test-job2"), lock = JobLock("lock-2"),
        weight = JobWeight(100), priority = JobPriority(3))

      val queuedJob1 = scheduledJob1.toQueuedJob(dummyTime.unsafeNow())
      val queuedJob2 = scheduledJob2.toQueuedJob(dummyTime.unsafeNow())

      val runningJob1 = queuedJob1.toRunningJobAndIncAttempts(NodeId("test-node-1"), dummyTime.unsafeNow())
      val runningJob2 = queuedJob2.toRunningJobAndIncAttempts(NodeId("test-node-2"), dummyTime.unsafeNow())

      jobsStore.reset()
      jobsScheduleProvider.reset()
      jobsScheduleProvider.scheduledJobs.append(scheduledJob1, scheduledJob2)

      val leader = createNewLeader()

      leader.electClusterLeader must beSucceedingTask
      leader.isLeader must beTrue
      leader.queueScheduledJobs must beSucceedingTask



      leader.queuedJobs.values().asScala must containTheSameElementsAs(List(queuedJob1, queuedJob2))
      jobsStore.queuedJobStore.values().asScala must containTheSameElementsAs(List(queuedJob1, queuedJob2))

      leader.assignQueuedJobs must beSucceedingTask

      leader.queuedJobs.values().asScala must beEmpty
      jobsStore.queuedJobStore.values().asScala must beEmpty

      leader.runningJobs.values().asScala must containTheSameElementsAs(List(runningJob1, runningJob2))
      jobsStore.runningJobStore.values().asScala must containTheSameElementsAs(List(runningJob1, runningJob2))
    }
  }

  "not assign jobs to any node if the least loaded node will be loaded with more than maxWeightPerNode ordered by priority and jobId" in {
    jobsStore.reset()
    jobsScheduleProvider.reset()

    val scheduledJob1 =
      scheduledJob.copy(id = JobId("test-job1"), lock = JobLock("lock-1"), weight = JobWeight(100), priority = JobPriority(2))
    val scheduledJob2 =
      scheduledJob.copy(id = JobId("test-job2"), lock = JobLock("lock-2"), weight = JobWeight(100), priority = JobPriority(3))
    val scheduledJob3 =
      scheduledJob.copy(id = JobId("test-job3"), lock = JobLock("lock-3"), weight = JobWeight(100), priority = JobPriority(2))
    val scheduledJob4 =
      scheduledJob.copy(id = JobId("test-job4"), lock = JobLock("lock-4"), weight = JobWeight(100), priority = JobPriority(1))


    val queuedJob2 = scheduledJob2.toQueuedJob(dummyTime.unsafeNow())
    val queuedJob3 = scheduledJob3.toQueuedJob(dummyTime.unsafeNow())

    val runningJob1 = scheduledJob1.toQueuedJob(dummyTime.unsafeNow())
      .toRunningJobAndIncAttempts(NodeId("test-node-2"), dummyTime.unsafeNow()) // second highest priority
    val runningJob4 = scheduledJob4.toQueuedJob(dummyTime.unsafeNow())
      .toRunningJobAndIncAttempts(NodeId("test-node-1"), dummyTime.unsafeNow()) // highest priority

    jobsScheduleProvider.scheduledJobs.append(
      scheduledJob1,
      scheduledJob2,
      scheduledJob3,
      scheduledJob4,
    )

    val leader = createNewLeader()
    leader.electClusterLeader must beSucceedingTask
    leader.isLeader must beTrue

    leader.queueScheduledJobs must beSucceedingTask
    leader.assignQueuedJobs must beSucceedingTask


    jobsStore.queuedJobStore.values().asScala must containTheSameElementsAs(List(queuedJob2, queuedJob3))
    jobsStore.runningJobStore.values().asScala must containTheSameElementsAs(List(runningJob1, runningJob4))
  }
}
