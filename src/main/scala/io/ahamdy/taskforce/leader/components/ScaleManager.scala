package io.ahamdy.taskforce.leader.components

import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicReference

import fs2.Task
import fs2.interop.cats._
import cats.syntax.flatMap._
import io.ahamdy.taskforce.api.{CloudManager, NodeInfoProvider}
import io.ahamdy.taskforce.common.Time
import io.ahamdy.taskforce.domain.{NodeActive, RunningJob}
import io.ahamdy.taskforce.store.NodeStore
import io.ahamdy.taskforce.syntax.zonedDateTime._
import io.ahamdy.taskforce.syntax.task._

import scala.concurrent.duration.FiniteDuration

trait ScaleManager {

}

class ScaleManagerImpl(cloudManager: CloudManager, nodeStore: NodeStore, config: ScaleManagerConfig, time: Time,
                       nodeInfoProvider: NodeInfoProvider) {
  val lastScaleActivity: AtomicReference[ZonedDateTime] = new AtomicReference(time.epoch)

  val scaleUpNeededSince: AtomicReference[Option[ZonedDateTime]] = new AtomicReference(None)
  val scaleDownNeededSince: AtomicReference[Option[ZonedDateTime]] = new AtomicReference(None)

  def scaleCluster(queuedAndRunningWeights: Int, activeNodesCapacity: Int): Task[Unit] = {
    time.now.flatMap { now =>
      if ((queuedAndRunningWeights / activeNodesCapacity) > config.scaleUpThreshold)
        scaleUpIfDue(now)
      else if ((queuedAndRunningWeights / activeNodesCapacity) < config.scaleDownThreshold)
        scaleDownIfDue(now)
      else
        Task.delay(scaleUpNeededSince.set(None)) >> Task.delay(scaleUpNeededSince.set(None))
    }
  }

  def scaleUpIfDue(now: ZonedDateTime): Task[Unit] =
    scaleUpNeededSince.get() match {
      case None => Task.delay(scaleUpNeededSince.set(Some(now))) >> Task.delay(scaleDownNeededSince.set(None))
      case Some(scaleUpNeededTime) if now.minus(scaleUpNeededTime) > config.evaluationPeriod =>
        nodeStore.getAllActiveNodesCountByGroup(nodeInfoProvider.nodeGroup).flatMap {
          case nodesCount if nodesCount < config.maxNodes =>
            cloudManager.scaleUp(Math.min(config.scaleUpStep, config.maxNodes - nodesCount)) >>
              Task.delay(lastScaleActivity.set(now)) >>
              Task.delay(scaleUpNeededSince.set(None)) >>
              Task.delay(scaleDownNeededSince.set(None))
          case nodesCount if nodesCount >= config.maxNodes =>
            Task.delay(scaleUpNeededSince.set(None))
        }
      case Some(_) => Task.unit
    }

  def scaleDownIfDue(now: ZonedDateTime): Task[Unit] =
    scaleDownNeededSince.get match {
      case None => Task.delay(scaleDownNeededSince.set(Some(now))) >> Task.delay(scaleUpNeededSince.set(None))
      case Some(scaleDownNeededTime) if now.minus(scaleDownNeededTime) > config.evaluationPeriod =>
        nodeStore.getAllActiveNodesCountByGroup(nodeInfoProvider.nodeGroup).flatMap {
          case nodesCount if nodesCount > config.minNodes =>
            nodeStore.getYoungestActiveNodesByGroup(nodeInfoProvider.nodeGroup, config.scaleDownStep)
              .map(_.map(node => nodeStore.updateNodeStatus(node.nodeId, NodeActive(false))))
              .flatMap(sequenceUnit) >>
              Task.delay(lastScaleActivity.set(now)) >>
              Task.delay(scaleUpNeededSince.set(None)) >>
              Task.delay(scaleDownNeededSince.set(None))
          case nodesCount if nodesCount <= config.minNodes =>
            Task.delay(scaleDownNeededSince.set(None))
        }
      case Some(_) => Task.delay(scaleUpNeededSince.set(None))
    }
}

case class ScaleManagerConfig(minNodes: Int, maxNodes: Int, coolDownPeriod: FiniteDuration, scaleDownThreshold: Int,
                              scaleUpThreshold: Int, evaluationPeriod: FiniteDuration, scaleUpStep: Int, scaleDownStep: Int)
