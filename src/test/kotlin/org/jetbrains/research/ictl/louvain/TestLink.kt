package org.jetbrains.research.ictl.louvain

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Polymorphic
@Serializable
sealed class TestLink : Link {
    abstract val source: Int
    abstract val target: Int
}

@Serializable
@SerialName("UnweightedLink")
class UnweightedLink(
    override val source: Int,
    override val target: Int,
) : TestLink() {
    override fun source() = source
    override fun target() = target
    override fun weight() = 1.0
}

@Serializable
@SerialName("WeightedLink")
class WeightedLink(
    override val source: Int,
    override val target: Int,
    val weight: Double
) : TestLink() {
    override fun source() = source
    override fun target() = target
    override fun weight() = weight
}
