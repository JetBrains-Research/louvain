package org.jetbrains.research.ictl.louvain

/**
 * Simple link representation used inside Node and Louvain algorithm.
 */
internal class InternalLink(
    val to: Int,
    val weight: Double
)

/**
 * @param community Index of a community to which the node has been assigned.
 * @param selfLoopsWeight Weight of all links that start and end at originalNodes making up this node * 2.
 */
internal sealed class BaseNode(
    var community: Int,
    val originalNodes: Set<Int>,
    open val incidentLinks: List<InternalLink>,
    open val selfLoopsWeight: Double
) {
    fun neighbourCommunities(nodes: List<Node>) = incidentLinks.map { nodes[it.to].community }.distinct().filter { it != community }
}

internal class Node(
    community: Int,
    originalNodes: Set<Int>,
    incidentLinks: List<InternalLink>,
    selfLoopsWeight: Double = 0.0
) : BaseNode(community, originalNodes, incidentLinks, selfLoopsWeight) {
    val outDegree = incidentLinks.sumOf { it.weight }
    fun degree() = outDegree + selfLoopsWeight
}

internal class MutableNode(
    community: Int,
    originalNodes: Set<Int>,
    override val incidentLinks: MutableList<InternalLink> = mutableListOf(),
    override var selfLoopsWeight: Double = 0.0
) : BaseNode(community, originalNodes, incidentLinks, selfLoopsWeight) {
    fun toNode(): Node = Node(community, originalNodes, incidentLinks)
}
