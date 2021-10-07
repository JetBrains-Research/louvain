package org.jetbrains.research.ictl.louvain

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Polymorphic
@Serializable
sealed class Link {
    abstract val source: Int
    abstract val target: Int
    abstract val weight: Double
}

@Serializable
@SerialName("UnweightedLink")
class UnweightedLink(
    override val source: Int,
    override val target: Int,
) : Link() {
    override val weight: Double
        get() = 1.0
}

@Serializable
@SerialName("WeightedLink")
class WeightedLink(
    override val source: Int,
    override val target: Int,
    override val weight: Double
) : Link()
