package org.jetbrains.research.ictl.louvain

/**
 * Runs Louvain algorithm to approximate the best modularity partition and returns a corresponding mapping of nodes to communities.
 * If depth > 0 then algorithm tries to split large communities into smaller ones depth times recursively.
 *
 * @param depth Number of attempts to split large communities
 * @return Map: nodeIndex -> communityIndex
 */
fun getPartition(links: List<Link>, depth: Int = 0): Map<Int, Int> {
    val louvain = Louvain(links)
    louvain.optimizeModularity(depth)
    return louvain.resultingCommunities()
}

/**
 * Computes modularity for a given graph with given partition. If no partition is given then each node has its own community.
 *
 * @param communitiesMap Map: nodeIndex -> communityIndex
 * @return Modularity
 * @throws AssertionError if communitiesMap contains negative community index or node index out of IntRange(0, (nodesCount - 1))
 */
fun computeModularity(links: List<Link>, communitiesMap: Map<Int, Int>? = null): Double {
    val louvain = Louvain(links)
    if (communitiesMap != null) {
        louvain.assignCommunities(communitiesMap)
    }
    return louvain.computeModularity()
}
