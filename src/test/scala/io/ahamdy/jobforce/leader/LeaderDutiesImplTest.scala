package io.ahamdy.jobforce.leader

import java.time.{ZoneId, ZonedDateTime}

import com.cronutils.model.{Cron, CronType}
import fs2.Task
import io.ahamdy.jobforce.domain._
import io.ahamdy.jobforce.scheduling.{CronLine, JobsScheduleProvider}
import io.ahamdy.jobforce.shared.NodeInfoProvider
import io.ahamdy.jobforce.store.NodeStore
import io.ahamdy.jobforce.testing.StandardSpec

import scala.concurrent.duration._


class LeaderDutiesImplTest extends StandardSpec{

  val nodeInfoProvider = new NodeInfoProvider {
    override def nodeGroup: NodeGroup = NodeGroup("test-group")
    override def nodeId: NodeId = NodeId("test-node-1")
  }

  val jobScheduleProvider = new JobsScheduleProvider {
    override def getJobsSchedule: Task[List[ScheduledJob]] =
      Task.now(List(
        ScheduledJob(JobLock("test-lock-1"),
          JobType("test-type-1"),
          JobWeight(5),
          Map("test-input-1" -> "1"),
          JobSchedule(CronLine.parse("0 0 7 ? * *", CronType.QUARTZ, ZoneId.of("UTC")).get, 1.minute),
          JobAttempts(0, 5),
          JobPriority(1)
      )))
  }

  val nodeStore = new NodeStore {
    override def getLeaderNodeByGroup(nodeGroup: NodeGroup): Task[JobNode] =
      Task.now(JobNode(NodeId("test-node-1"), NodeGroup("test-group"), ZonedDateTime.now(), NodeActive(true), NodeVersion("1.0.0")))

    override def getAllNodes: Task[List[JobNode]] = ???

    override def getAllNodesByGroup(groupName: NodeGroup): Task[List[JobNode]] = ???
  }

  val leader = new LeaderDutiesImpl(
    JobForceLeaderConfig(100, 1.second, true),
    nodeInfoProvider,
    jobScheduleProvider,


  )

  "LeaderDutiesImpl" should  {
    "electClusterLeader" in {

    }
  }
}
