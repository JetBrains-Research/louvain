package org.jetbrains.research.ictl.louvain

/**
 * Class that encapsulates the Louvain algorithm.
 */
internal class Louvain(
    private val links: List<Link>
) {
    private var communities: MutableMap<Int, Community>
    private var nodes: List<Node> = emptyList()
    private var graphWeight: Double
    private val originalNodesNumber: Int

    init {
        buildNodesFromLinks()
        originalNodesNumber = nodes.size
        communities = nodes.withIndex().associate { it.index to Community(it.index, nodes) }.toMutableMap()
        graphWeight = computeGraphWeight()
    }

    private fun buildNodesFromLinks() {
        val nodeIndices = links.flatMap { listOf(it.source(), it.target()) }.distinct().sorted()
        val mutableNodes = nodeIndices
            .withIndex()
            .associateBy({ it.value }, { MutableNode(it.index, setOf(it.value)) })
        links.forEach { link ->
            if (link.source() == link.target()) {
                mutableNodes[link.source()]!!.selfLoopsWeight += 2 * link.weight()
            } else {
                val newSource = mutableNodes[link.source()]!!.community
                val newTarget = mutableNodes[link.target()]!!.community
                mutableNodes[link.source()]!!.incidentLinks.add(InternalLink(newTarget, link.weight()))
                mutableNodes[link.target()]!!.incidentLinks.add(InternalLink(newSource, link.weight()))
            }
        }
        nodes = mutableNodes.values.map { it.toNode() }
    }

    private fun computeGraphWeight() =
        nodes.sumOf { n -> n.incidentLinks.sumOf { l -> l.weight } + n.selfLoopsWeight } / 2

    private fun aggregateCommunities() {
        // re-index communities in nodes
        communities.values.withIndex().forEach { (newIndex, community) ->
            community.nodes.forEach { nodeIndex ->
                nodes[nodeIndex].community = newIndex
            }
        }

        val newNodes = communities.values.map { it.toLouvainNode(nodes) }
        val newCommunities =
            newNodes.withIndex().associateBy({ it.index }, { Community(it.index, nodes) }).toMutableMap()

        nodes = newNodes
        communities = newCommunities
    }

    private fun moveNode(nodeIndex: Int, node: Node, toCommunityIndex: Int) {
        val from = communities[node.community]!!
        if (from.removeNode(nodeIndex, nodes)) {
            communities.remove(node.community)
        }
        node.community = toCommunityIndex
        communities[toCommunityIndex]!!.addNode(nodeIndex, nodes)
    }

    private fun computeCostOfMovingOut(index: Int, node: Node): Double {
        val theCommunity = communities[node.community]!!
        theCommunity.removeNode(index, nodes)
        val cost = theCommunity.modularityChangeIfNodeAdded(node, graphWeight)
        theCommunity.addNode(index, nodes)
        return cost
    }

    /**
     * Step I of the algorithm:
     * For each node i evaluate the gain in modularity if node i is moved to the community of one of its neighbors j.
     * Then move node i in the community for which the modularity gain is the largest, but only if this gain is positive.
     * This process is applied to all nodes until no further improvement can be achieved, completing Step I.
     * @see optimizeModularity
     */
    private fun findLocalMaxModularityPartition() {
        var repeat = true
        while (repeat) {
            repeat = false
            for ((i, node) in nodes.withIndex()) {
                var bestCommunity = node.community
                var maxDeltaM = 0.0
                val costOfMovingOut = computeCostOfMovingOut(i, node)
                for (communityIndex in node.neighbourCommunities(nodes)) {
                    if (communityIndex == node.community) {
                        continue
                    }
                    val toCommunity = communities[communityIndex]!!
                    val deltaM = toCommunity.modularityChangeIfNodeAdded(node, graphWeight) - costOfMovingOut
                    if (deltaM > maxDeltaM) {
                        bestCommunity = communityIndex
                        maxDeltaM = deltaM
                    }
                }
                if (bestCommunity != node.community) {
                    moveNode(i, node, bestCommunity)
                    repeat = true
                }
            }
        }
    }

    fun computeModularity() = communities.values.sumOf { it.computeModularity(graphWeight) }

    fun optimizeModularity(depth: Int = 0) {
        var bestModularity = computeModularity()
        var bestCommunities = communities
        var bestNodes = nodes
        do {
            val from = communities.size
            findLocalMaxModularityPartition()
            aggregateCommunities()
            val newModularity = computeModularity()
            if (newModularity > bestModularity) {
                bestModularity = newModularity
                bestCommunities = communities
                bestNodes = nodes
            }
        } while (communities.size != from)
        communities = bestCommunities
        nodes = bestNodes
        if (communities.size != 1 && depth != 0) {
            refine(depth)
        }
    }

    fun resultingCommunities(): Map<Int, Int> {
        val communitiesMap = mutableMapOf<Int, Int>()
        communities.forEach { (communityIndex, community) ->
            community.nodes.forEach { nodeIndex ->
                val node = nodes[nodeIndex]
                node.originalNodes.forEach {
                    communitiesMap[it] = communityIndex
                }
            }
        }
        return communitiesMap
    }

    fun assignCommunities(communitiesMap: Map<Int, Int>) {
        communities.clear()
        buildNodesFromLinks()
        buildCommunitiesFromMap(communitiesMap)
    }

    private fun buildCommunitiesFromMap(communitiesMap: Map<Int, Int>) {
        // create all necessary communities
        for (entry in communitiesMap) {
            val communityIndex = entry.value
            if (communityIndex !in communities.keys) {
                communities[communityIndex] = Community()
            }
        }

        val nodeIndicesMap = communitiesMap.keys.sorted().withIndex().associateBy({ it.value }, { it.index })

        // distribute the nodes among communities
        for (entry in communitiesMap) {
            val nodeIndex = nodeIndicesMap[entry.key]!!
            val communityIndex = entry.value

            nodes[nodeIndex].community = -1
            communities[communityIndex]!!.addNode(nodeIndex, nodes)
            nodes[nodeIndex].community = communityIndex
        }
    }

    private fun refine(depth: Int = 0) {
        var communitiesMap = resultingCommunities()
        var resultingCommunitiesNumber = communitiesMap.values.distinct().size
        links
            .filter { communitiesMap[it.source()] == communitiesMap[it.target()] }
            .groupBy({ communitiesMap[it.source()]!! }, { it })
            .filter { communities[it.key]!!.overResolutionLimit(graphWeight) }
            .forEach { (communityIndex, links) ->
                val thisLouvain = Louvain(links)
                thisLouvain.optimizeModularity(depth - 1)
                val thisMap = thisLouvain.resultingCommunities()
                val reindex = reIndexMap(thisMap, communityIndex, resultingCommunitiesNumber)
                communitiesMap = communitiesMap + reindex
                resultingCommunitiesNumber = communitiesMap.values.distinct().size
            }
        assignCommunities(communitiesMap)
    }

    private fun reIndexMap(theMap: Map<Int, Int>, saveIndex: Int, startFrom: Int) = theMap
        .mapValues { (_, communityIndex) ->
            if (communityIndex == 0) {
                saveIndex
            } else {
                communityIndex + startFrom - 1
            }
        }
}
