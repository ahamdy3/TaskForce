package io.ahamdy.jobforce.leader.components

import io.ahamdy.jobforce.domain._

object NodeLoadBalancer {

  def leastLoadedNode(runningJobs: List[RunningJob],
                      allActiveNodes: List[JobNode],
                      versionRule: JobVersionRule,
                      leaderNodeId: NodeId,
                      leaderAlsoWorker: Boolean,
                      position: Int): Option[NodeLoad] = {
    val nodeLoadOrdering = Ordering.by((x: NodeLoad) =>
      (x.jobsWeight, x.node.startTime.toInstant.toEpochMilli, x.node.nodeId.value))
    val activeNodesMap = allActiveNodes.map(node => node.nodeId -> node).toMap

    val activeLoads = runningJobs.collect {
      case job if activeNodesMap.contains(job.nodeId) =>
        NodeLoad(activeNodesMap(job.nodeId), job.weight.value)
    }

    val allNodesLoad = allActiveNodes.map(node => NodeLoad(node, 0))

    val (activeWorkerLoads, allWorkersLoad) =
      if (leaderAlsoWorker)
        (activeLoads, allNodesLoad)
      else
        (activeLoads.filterNot(_.node.nodeId == leaderNodeId),
          allNodesLoad.filterNot(_.node.nodeId == leaderNodeId))


    combineNodeLoads(activeWorkerLoads, allWorkersLoad)
      .filter(nodeLoad => NodeVersionChecker.checkVersion(versionRule, nodeLoad.node)) match {
      case nodes if position < nodes.length => Some(nodes.sorted(nodeLoadOrdering)(position))
      case _ => None
    }
  }

  def combineNodeLoads(list1: List[NodeLoad], list2: List[NodeLoad]): List[NodeLoad] =
    (list1 ++ list2).groupBy(_.node).map { case (node, nodes) => NodeLoad(node, nodes.map(_.jobsWeight).sum) }.toList

  def canNodeHandle(nodeCurrentWeight: Int, jobWeight: Int, maxWeightPerNode: Int): Boolean =
    (nodeCurrentWeight + Math.min(jobWeight, maxWeightPerNode)) <= maxWeightPerNode
}
