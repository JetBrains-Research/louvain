package org.jetbrains.research.ictl.louvain

import kotlin.math.pow
import kotlin.math.sqrt

internal fun Community(nodeIndex: Int, graph: List<Node>): Community {
    val node = graph[nodeIndex]
    return Community(node.selfLoopsWeight, node.outDegree, mutableSetOf(nodeIndex))
}

internal class Community(
    private var selfLoopsWeight: Double = 0.0,
    private var outLinksWeight: Double = 0.0,
    val nodes: MutableSet<Int> = mutableSetOf()
) {

    private fun totalWeightsSum() = selfLoopsWeight + outLinksWeight

    fun addNode(index: Int, nodes: List<Node>) {
        val node = nodes[index]
        node.incidentLinks.forEach { link ->
            if (link.to in this.nodes) {
                selfLoopsWeight += 2 * link.weight
                outLinksWeight -= link.weight
            } else {
                outLinksWeight += link.weight
            }
        }
        selfLoopsWeight += node.selfLoopsWeight
        this.nodes.add(index)
    }

    fun removeNode(index: Int, nodes: List<Node>): Boolean {
        val node = nodes[index]
        node.incidentLinks.forEach { link ->
            if (link.to in this.nodes) {
                selfLoopsWeight -= 2 * link.weight
                outLinksWeight += link.weight
            } else {
                outLinksWeight -= link.weight
            }
        }
        this.nodes.remove(index)
        selfLoopsWeight -= node.selfLoopsWeight
        return this.nodes.size == 0
    }

    fun modularityChangeIfNodeAdded(node: Node, graphWeight: Double): Double =
        (1 / graphWeight) * (weightsToNode(node) - totalWeightsSum() * node.degree() / (2 * graphWeight))

    private fun weightsToNode(node: Node): Double = node.incidentLinks.filter { it.to in nodes }.sumOf { it.weight }

    fun computeModularity(graphWeight: Double): Double = (selfLoopsWeight / (2 * graphWeight)) - (totalWeightsSum() / (2 * graphWeight)).pow(2)

    fun toLouvainNode(nodes: List<Node>): Node {
        val newIndex = nodes[this.nodes.first()].community
        val consumedNodes = this.nodes.flatMap { nodes[it].originalNodes }.toSet()
        var newSelfLoopsWeight = 0.0

        val incidentLinksMap = mutableMapOf<Int, Double>()
        this.nodes.forEach { nodeIndex ->
            newSelfLoopsWeight += nodes[nodeIndex].selfLoopsWeight
            nodes[nodeIndex].incidentLinks.forEach { link ->
                val toNewNode = nodes[link.to].community
                if (toNewNode != newIndex) {
                    if (toNewNode in incidentLinksMap) {
                        incidentLinksMap[toNewNode] = incidentLinksMap[toNewNode]!! + link.weight
                    } else {
                        incidentLinksMap[toNewNode] = link.weight
                    }
                } else {
                    newSelfLoopsWeight += link.weight
                }
            }
        }
        val links = incidentLinksMap.map { InternalLink(it.key, it.value) }

        return Node(newIndex, consumedNodes, links, newSelfLoopsWeight)
    }

    /**
     * If communities size is less than sqrt(2 * graphWeight) then merging it with another one will always increase modularity.
     * Hence, if community size is greater than sqrt(2 * graphWeight), it might actually consist of several smaller communities.
     */
    fun overResolutionLimit(graphWeight: Double): Boolean = selfLoopsWeight >= sqrt(2 * graphWeight)
}
