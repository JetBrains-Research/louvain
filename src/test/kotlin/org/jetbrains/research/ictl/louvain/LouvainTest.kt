package org.jetbrains.research.ictl.louvain

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File
import kotlin.math.round
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class LouvainTest {
    private fun genFullyConnected(indicesFrom: Int, size: Int): List<Link> {
        val links: MutableList<Link> = mutableListOf()
        IntRange(indicesFrom, indicesFrom + size - 1).forEach { i ->
            IntRange(i + 1, indicesFrom + size - 1).forEach { j ->
                links.add(UnweightedLink(i, j))
            }
        }
        return links
    }

    private fun getResourcePath(name: String) =
        this.javaClass.classLoader.getResource(name)?.path ?: "src/test/resources/$name"

    private fun genGraph(size: Int, communities: Int): List<Link> {
        val links: MutableList<Link> = mutableListOf()
        val communitySize = size / communities
        IntRange(0, communities - 1).forEach { communityIndex ->
            links.addAll(genFullyConnected(communityIndex * communitySize, communitySize))
            val nextCommunityIndex = (communityIndex + 1) % communities
            links.add(UnweightedLink(communityIndex * communitySize, nextCommunityIndex * communitySize))
            links.add(UnweightedLink(communityIndex * communitySize + 1, nextCommunityIndex * communitySize + 1))
        }
        return links
    }

    private fun Double.round2(): Double = round(this * 100) / 100

    private fun linksFromStubs(filename: String) =
        Json.decodeFromString<List<UnweightedLink>>(File(filename).readText())

    private fun checkStartModularity(actualModularity: Double) {
        assertTrue(actualModularity < 0.0, "Start modularity should be less than zero")
    }

    private fun checkModularity(expectedModularity: Double?, actualModularity: Double) {
        if (expectedModularity != null) {
            assertEquals(expectedModularity, actualModularity)
        }
    }

    private fun checkPartitionSize(expectedSize: Int, actualSize: Int) {
        assertEquals(expectedSize, actualSize)
    }

    private fun checkRefined(
        links: List<Link>,
        expectedCommunitiesNumber: Int?
    ) {
        if (expectedCommunitiesNumber != null) {
            val afterRefineCommunities = getPartition(links, -1)
            assertEquals(expectedCommunitiesNumber, afterRefineCommunities.values.distinct().size)
        }
    }

    private fun test(
        links: List<Link>,
        expectedCommunitiesNumber: Int,
        expectedModularity: Double? = null,
        expectedCommunitiesNumberAfterRefine: Int? = null
    ) {
        checkStartModularity(computeModularity(links))
        val communitiesMap = getPartition(links)

        checkModularity(expectedModularity, computeModularity(links, communitiesMap).round2())

        checkPartitionSize(expectedCommunitiesNumber, communitiesMap.values.distinct().size)

        checkRefined(links, expectedCommunitiesNumberAfterRefine)
    }

    @Test
    fun threeConnectedNodes() = test(linksFromStubs(getResourcePath("test0.json")), 1, 0.0, 1)

    @Test
    fun fourNodesTwoCommunities() = test(linksFromStubs(getResourcePath("test1.json")), 2, 0.5, 2)

    /**
     * This test is based on a graph from "Network Science" by Albert-Laszlo Barabasi, Image 9.16
     */
    @Test
    fun testBarabasi_9_16() = test(linksFromStubs(getResourcePath("test2.json")), 2, 0.41)

    /**
     * This test is based on a graph from "Network Science" by Albert-Laszlo Barabasi, Image 9.37
     */
    @Test
    fun testBarabasi_9_37() = test(linksFromStubs(getResourcePath("test3.json")), 2)

    /**
     * Generate 10 fully connected graphs of size 10 and connect them in a circle with two links from each graph to the next
     */
    @Test
    fun test10GeneratedCommunities() = test(genGraph(100, 10), 10, expectedCommunitiesNumberAfterRefine = 10)

    /**
     * This test illustrates Louvain resolution limit problem
     */
    @Test
    fun testResolutionLimit() = test(genGraph(1000, 100), 50, expectedCommunitiesNumberAfterRefine = 100)
}
