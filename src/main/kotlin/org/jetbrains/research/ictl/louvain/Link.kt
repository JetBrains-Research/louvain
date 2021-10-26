package org.jetbrains.research.ictl.louvain

interface Link {
    fun source(): Int
    fun target(): Int
    fun weight(): Double
}
