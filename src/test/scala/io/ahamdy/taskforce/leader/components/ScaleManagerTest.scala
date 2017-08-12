package io.ahamdy.taskforce.leader.components

import java.time.ZonedDateTime

import io.ahamdy.taskforce.api.{DummyCloudManager, DummyNodeInfoProvider}
import io.ahamdy.taskforce.common.DummyTime
import io.ahamdy.taskforce.domain._
import io.ahamdy.taskforce.store.DummyNodeStore
import io.ahamdy.taskforce.testing.StandardSpec
import io.ahamdy.taskforce.syntax.zonedDateTime._

import scala.concurrent.duration._

class ScaleManagerTest extends StandardSpec {
  val config = ScaleManagerConfig(
    minNodes = 1,
    maxNodes = 5,
    coolDownPeriod = 1.minute,
    scaleDownThreshold = 30,
    scaleUpThreshold = 80,
    evaluationPeriod = 1.minute,
    scaleUpStep = 2,
    scaleDownStep = 2)

  val nodeInfoProvider = new DummyNodeInfoProvider("node-1", "test-group")

  "ScaleManagerTest" should {
    "scaleUpIfDue" should {
      "mark scaleUpNeededSince by now if it was None and do nothing to cluster" in {
        val time = new DummyTime(ZonedDateTime.now())
        val cloudManager = new DummyCloudManager(initialNodesCount = 2)
        val nodeStore = new DummyNodeStore(time, nodeInfoProvider.nodeGroup)
        val scaleManager = new ScaleManagerImpl(config, cloudManager, nodeInfoProvider, nodeStore, time)

        scaleManager.scaleUpNeededSince.get must beNone
        cloudManager.nodesCounter.get mustEqual 2

        scaleManager.scaleUpIfDue(time.unsafeNow()) must beSucceedingTask

        scaleManager.scaleUpNeededSince.get must beSome(time.unsafeNow())
        cloudManager.nodesCounter.get mustEqual 2
      }

      "do nothing if evaluationPeriod has not been exceeded yet since scaleUpNeededSince" in {
        val time = new DummyTime(ZonedDateTime.now())
        val cloudManager = new DummyCloudManager(initialNodesCount = 2)
        val nodeStore = new DummyNodeStore(time, nodeInfoProvider.nodeGroup)
        val scaleManager = new ScaleManagerImpl(config, cloudManager, nodeInfoProvider, nodeStore, time)

        scaleManager.scaleUpIfDue(time.unsafeNow()) must beSucceedingTask

        val scaleUpNeededSince = time.unsafeNow()
        scaleManager.scaleUpNeededSince.get must beSome(scaleUpNeededSince)
        scaleManager.lastScaleActivity.get mustEqual time.epoch
        cloudManager.nodesCounter.get mustEqual 2

        time.currentTime.set(time.unsafeNow().plus(config.evaluationPeriod).minus(1.second))

        scaleManager.scaleUpIfDue(time.unsafeNow()) must beSucceedingTask

        scaleManager.scaleUpNeededSince.get must beSome(scaleUpNeededSince)
        scaleManager.lastScaleActivity.get mustEqual time.epoch
        cloudManager.nodesCounter.get mustEqual 2
      }

      "do nothing if maxNodes has been reached" in {
        val time = new DummyTime(ZonedDateTime.now())
        val cloudManager = new DummyCloudManager(initialNodesCount = 2)
        val nodeStore = new DummyNodeStore(time, nodeInfoProvider.nodeGroup)
        val scaleManager = new ScaleManagerImpl(config.copy(maxNodes = 2), cloudManager, nodeInfoProvider, nodeStore, time)

        scaleManager.scaleUpIfDue(time.unsafeNow()) must beSucceedingTask

        val scaleUpNeededSince = time.unsafeNow()
        scaleManager.scaleUpNeededSince.get must beSome(scaleUpNeededSince)
        scaleManager.lastScaleActivity.get mustEqual time.epoch
        cloudManager.nodesCounter.get mustEqual 2

        time.currentTime.set(time.unsafeNow().plus(config.evaluationPeriod))

        scaleManager.scaleUpIfDue(time.unsafeNow()) must beSucceedingTask

        scaleManager.scaleUpNeededSince.get must beSome(scaleUpNeededSince)
        scaleManager.lastScaleActivity.get mustEqual time.epoch
        cloudManager.nodesCounter.get mustEqual 2
      }

      "scale up cluster only if evaluationPeriod has been exceeded since scaleUpNeededSince and nodes < maxNodes using scaleUpStep" in {
        val time = new DummyTime(ZonedDateTime.now())
        val cloudManager = new DummyCloudManager(initialNodesCount = 2)
        val nodeStore = new DummyNodeStore(time, nodeInfoProvider.nodeGroup)
        val scaleManager = new ScaleManagerImpl(config, cloudManager, nodeInfoProvider, nodeStore, time)

        scaleManager.scaleUpIfDue(time.unsafeNow()) must beSucceedingTask

        time.currentTime.set(time.unsafeNow().plus(config.evaluationPeriod))

        scaleManager.scaleUpIfDue(time.unsafeNow()) must beSucceedingTask

        scaleManager.scaleUpNeededSince.get must beNone
        scaleManager.lastScaleActivity.get mustEqual time.unsafeNow()
        cloudManager.nodesCounter.get mustEqual (2 + config.scaleUpStep)
      }

      "scale up cluster using remaining cluster capacity if scaleUpStep > remaining capacity" in {
        val time = new DummyTime(ZonedDateTime.now())
        val cloudManager = new DummyCloudManager(initialNodesCount = 2)
        val nodeStore = new DummyNodeStore(time, nodeInfoProvider.nodeGroup)
        val scaleConfig = config.copy(maxNodes = 5, scaleUpStep = 4)
        val scaleManager = new ScaleManagerImpl(scaleConfig, cloudManager, nodeInfoProvider, nodeStore, time)

        scaleManager.scaleUpIfDue(time.unsafeNow()) must beSucceedingTask
        time.currentTime.set(time.unsafeNow().plus(config.evaluationPeriod))
        scaleManager.scaleUpIfDue(time.unsafeNow()) must beSucceedingTask

        cloudManager.nodesCounter.get mustEqual scaleConfig.maxNodes
      }
    }

    "scaleDownIfDue" should {
      "mark scaleDownNeededSince by now if it was None and do nothing to cluster" in {
        val time = new DummyTime(ZonedDateTime.now())
        val cloudManager = new DummyCloudManager(initialNodesCount = 2)
        val nodeStore = new DummyNodeStore(time, nodeInfoProvider.nodeGroup)
        val scaleManager = new ScaleManagerImpl(config, cloudManager, nodeInfoProvider, nodeStore, time)

        scaleManager.scaleDownNeededSince.get must beNone
        nodeStore.getAllActiveNodesCountByGroup(nodeInfoProvider.nodeGroup) must beSucceedingTask(2)

        scaleManager.scaleDownIfDue(time.unsafeNow()) must beSucceedingTask

        scaleManager.scaleDownNeededSince.get must beSome(time.unsafeNow())
        nodeStore.getAllActiveNodesCountByGroup(nodeInfoProvider.nodeGroup) must beSucceedingTask(2)
        scaleManager.lastScaleActivity.get mustEqual time.epoch
      }

      "do nothing if evaluationPeriod has not been exceeded yet since scaleDownNeededSince" in {
        val time = new DummyTime(ZonedDateTime.now())
        val cloudManager = new DummyCloudManager(initialNodesCount = 2)
        val nodeStore = new DummyNodeStore(time, nodeInfoProvider.nodeGroup)
        val scaleManager = new ScaleManagerImpl(config, cloudManager, nodeInfoProvider, nodeStore, time)
        val scaleDownIfDue = time.unsafeNow()

        scaleManager.scaleDownIfDue(scaleDownIfDue) must beSucceedingTask

        scaleManager.scaleDownNeededSince.get must beSome(scaleDownIfDue)
        nodeStore.getAllActiveNodesCountByGroup(nodeInfoProvider.nodeGroup) must beSucceedingTask(2)

        time.currentTime.set(time.unsafeNow().plus(config.evaluationPeriod).minus(1.second))

        scaleManager.scaleDownIfDue(scaleDownIfDue) must beSucceedingTask

        scaleManager.scaleDownNeededSince.get must beSome(scaleDownIfDue)
        nodeStore.getAllActiveNodesCountByGroup(nodeInfoProvider.nodeGroup) must beSucceedingTask(2)
      }

      "do nothing if minNodes has been reached" in {
        val time = new DummyTime(ZonedDateTime.now())
        val cloudManager = new DummyCloudManager(initialNodesCount = 2)
        val nodeStore = new DummyNodeStore(time, nodeInfoProvider.nodeGroup)
        val scaleManager = new ScaleManagerImpl(config.copy(minNodes = 2), cloudManager, nodeInfoProvider, nodeStore, time)

        scaleManager.scaleDownIfDue(time.unsafeNow()) must beSucceedingTask

        val scaleDownNeededSince = time.unsafeNow()
        scaleManager.scaleDownNeededSince.get must beSome(scaleDownNeededSince)
        scaleManager.lastScaleActivity.get mustEqual time.epoch
        nodeStore.getAllActiveNodesCountByGroup(nodeInfoProvider.nodeGroup) must beSucceedingTask(2)

        time.currentTime.set(time.unsafeNow().plus(config.evaluationPeriod))

        scaleManager.scaleDownIfDue(time.unsafeNow()) must beSucceedingTask

        scaleManager.scaleDownNeededSince.get must beSome(scaleDownNeededSince)
        scaleManager.lastScaleActivity.get mustEqual time.epoch
        nodeStore.getAllActiveNodesCountByGroup(nodeInfoProvider.nodeGroup) must beSucceedingTask(2)
      }

      "scale down using scaleDownStep only if evaluationPeriod has been exceeded since scaleUpNeededSince and nodes > minNodes" in {
        val time = new DummyTime(ZonedDateTime.now())
        val cloudManager = new DummyCloudManager(initialNodesCount = 3)
        val nodeStore = new DummyNodeStore(time, nodeInfoProvider.nodeGroup)
        nodeStore.nodesList.set(List(
          JobNode(NodeId("test-node-1"), NodeGroup("test-group"), time.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0")),
          JobNode(NodeId("test-node-2"), NodeGroup("test-group"), time.unsafeNow(), NodeActive(true), NodeVersion("1.0.0")),
          JobNode(NodeId("test-node-3"), NodeGroup("test-group"), time.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0"))))
        val scaleManager = new ScaleManagerImpl(config, cloudManager, nodeInfoProvider, nodeStore, time)

        nodeStore.getAllActiveNodesCountByGroup(nodeInfoProvider.nodeGroup) must beSucceedingTask(3)

        scaleManager.scaleDownIfDue(time.unsafeNow()) must beSucceedingTask

        time.currentTime.set(time.unsafeNow().plus(config.evaluationPeriod))

        scaleManager.scaleDownIfDue(time.unsafeNow()) must beSucceedingTask

        scaleManager.scaleDownNeededSince.get must beNone
        scaleManager.lastScaleActivity.get mustEqual time.unsafeNow()
        nodeStore.getAllActiveNodesCountByGroup(nodeInfoProvider.nodeGroup) must beSucceedingTask(3 - config.scaleDownStep)
      }

      "scale down cluster using remaining cluster capacity if scaleDownStep > remaining number to reach minNodes" in {
        val time = new DummyTime(ZonedDateTime.now())
        val cloudManager = new DummyCloudManager(initialNodesCount = 3)
        val nodeStore = new DummyNodeStore(time, nodeInfoProvider.nodeGroup)
        nodeStore.nodesList.set(List(
          JobNode(NodeId("test-node-1"), NodeGroup("test-group"), time.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0")),
          JobNode(NodeId("test-node-2"), NodeGroup("test-group"), time.unsafeNow(), NodeActive(true), NodeVersion("1.0.0")),
          JobNode(NodeId("test-node-3"), NodeGroup("test-group"), time.unsafeNow().minusMinutes(1), NodeActive(true), NodeVersion("1.0.0"))))
        val scaleConfig = config.copy(minNodes = 2, scaleDownStep = 4)
        val scaleManager = new ScaleManagerImpl(scaleConfig, cloudManager, nodeInfoProvider, nodeStore, time)

        nodeStore.getAllActiveNodesCountByGroup(nodeInfoProvider.nodeGroup) must beSucceedingTask(3)

        scaleManager.scaleDownIfDue(time.unsafeNow()) must beSucceedingTask
        time.currentTime.set(time.unsafeNow().plus(config.evaluationPeriod))
        scaleManager.scaleDownIfDue(time.unsafeNow()) must beSucceedingTask

        nodeStore.getAllActiveNodesCountByGroup(nodeInfoProvider.nodeGroup) must beSucceedingTask(scaleConfig.minNodes)
      }
    }

    "scaleCluster" in {
      ok
    }

    "cleanInactiveNodes" in {
      ok
    }

  }


}
