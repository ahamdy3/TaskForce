package io.ahamdy.jobforce

import java.util.concurrent.ConcurrentHashMap

import cats.implicits._
import fs2.interop.cats._
import fs2.Task
import io.ahamdy.jobforce.common.{Logging, Time}


trait JobsSynchronizer {
  def processIfAvailable(partner: PartnerId, feedType: FeedType, job: Task[Unit],
                         executeOnCurrentNode: Boolean = false): Task[Unit]
  def getCurrentNodeJobs: Task[List[NodeJob]]
  def unlockAllJobsByGroupName(groupName: Option[NodeGroupName]): Task[Unit]
}

class JobsSynchronizerImpl(config: JobsSynchronizerConfig, nodeInfoClient: NodeInfoClient, nodeJobStore: NodeJobStore,
                           currentNodesStore: CurrentNodesStore, time: Time)
  extends JobsSynchronizer with Logging {

  val localJobs = new ConcurrentHashMap[PartnerId, NodeJob]

  def processIfAvailable(partnerId: PartnerId, feedType: FeedType, job: Task[Unit],
                         executeOnCurrentNode: Boolean = false): Task[Unit] =
    lockJob(partnerId, feedType, executeOnCurrentNode).flatMap { isAvailable =>
      if (isAvailable) {
        for {
          _ <- logInfo(s"Launching job for partner: ${partnerId.value} of feedType: ${feedType.entryName} on node: ${nodeInfoClient.nodeId}")
          _ <- job.onError { err =>
                //IMPORTANT: safety net logging
                logError(s"Feed Failure: of $partnerId $feedType on ${nodeInfoClient.nodeId}: $err")
              }.attempt
          _ <- unlockJob(partnerId, feedType)
        } yield ()
      } else {
        logDebug(s"Skipping job for partner: ${partnerId.value} of feedType: ${feedType.entryName} on node: ${nodeInfoClient.nodeId}")
      }
    }

  def lockJob(partnerId: PartnerId, feedType: FeedType, executeOnCurrentNode: Boolean): Task[Boolean] = {

    def takeJobIfAvailable(nodeJob: NodeJob): Task[Boolean] =
      Task.now(nodeJob.nodeId == nodeInfoClient.nodeId && nodeJob.feedType == feedType &&
        Option(localJobs.putIfAbsent(partnerId, nodeJob)).isEmpty)

    def createNewJob(nodes: List[NodeId]): Task[Boolean] =
      nodeJobStore.getAllJobsByGroupName(nodeInfoClient.groupName).flatMap { currentJobs =>
        leastLoadedNode(currentJobs, nodeInfoClient.nodeId, nodes) match {
          case NodeLoad(happiestNode, jobsPoints) if executeOnCurrentNode || jobsPoints < config.maxWeightPerInstance =>
            for {
              now <- time.now
              nodeJob = NodeJob(nodeInfoClient.groupName, happiestNode, partnerId, feedType, now)
              isLocked <- nodeJobStore.lockJob(nodeJob)
            } yield isLocked && (executeOnCurrentNode || happiestNode == nodeInfoClient.nodeId) &&
              Option(localJobs.putIfAbsent(partnerId, nodeJob)).isEmpty
          case NodeLoad(happiestNode, jobsPoints) =>
            logInfo(s"Happiest Node $happiestNode load is already exceeded, current load points: $jobsPoints, max points: ${config.maxWeightPerInstance}")
              .map(_ => false)
        }
      }

    def checkRemoteJob: Task[Boolean] =
      currentNodesStore.getAllNodesByGroupName(nodeInfoClient.groupName)
        .flatMap {
          case nodes if !executeOnCurrentNode && nodes.size < config.minimumNodes =>
            logInfo(s"Cluster is not ready yet, current size: ${nodes.size} node(s)")
              .followedBy(Task.now(false))
          case nodes =>
            nodeJobStore.cleanJobs
              .followedBy(nodeJobStore.getJob(partnerId))
              .flatMap(_.map(takeJobIfAvailable).getOrElse(createNewJob(nodes)))
        }

    if(localJobs.containsKey(partnerId))
      Task.now(false)
    else
      checkRemoteJob
  }

  def unlockJob(partner: PartnerId, feedType: FeedType): Task[Unit] =
    Task.delay(localJobs.remove(partner))
      .followedBy(nodeJobStore.unlockJob(nodeInfoClient.nodeId, partner, feedType))
      .followedBy(logDebug(s"Unlocked job for $partner, $feedType"))


  override def getCurrentNodeJobs: Task[List[NodeJob]] =
    nodeJobStore.getAllJobs

  override def unlockAllJobsByGroupName(groupNameOpt: Option[NodeGroupName]): Task[Unit] =
    groupNameOpt match {
      case Some(groupName) if groupName == nodeInfoClient.groupName =>
        Task.failing("Unlocking current group jobs is not allowed")
      case Some(groupName) =>
        nodeJobStore.unlockAllJobsByGroupName(groupName)
      case None =>
        nodeJobStore.unlockAllOtherGroupsJobs(nodeInfoClient.groupName)
    }

  def leastLoadedNode(nodeJobs: List[NodeJob], currentNodeId: NodeId, allNodes: List[NodeId]): NodeLoad = {
    val nodeLoadOrdering = Ordering.by((x: NodeLoad) => (x.jobsCount, x.nodeId.value))

    nodeJobs match {
      case  Nil => NodeLoad(currentNodeId, 0)
      case _ =>
        val activeNodesLoad = nodeJobs.map {
          case NodeJob(_, nodeId, _, FeedType.Catalog, _) => NodeLoad(nodeId, config.catalogJobWeight)
          case NodeJob(_, nodeId, _, FeedType.Stock, _) => NodeLoad(nodeId, config.stockJobWeight)
        }
        val allNodesLoad = allNodes.map(nodeId => NodeLoad(nodeId, 0))

        combineNodeLoads(activeNodesLoad, allNodesLoad)
          .min(nodeLoadOrdering)
    }
  }

  private def combineNodeLoads(list1: List[NodeLoad], list2: List[NodeLoad]): List[NodeLoad] =
    (list1 ++ list2).groupBy(_.nodeId).map { case (nodeId, nodes) => NodeLoad(nodeId, nodes.map(_.jobsCount).sum) }.toList

}

case class JobsSynchronizerConfig(minimumNodes: Int, catalogJobWeight: Int, stockJobWeight: Int, maxWeightPerInstance: Int)
