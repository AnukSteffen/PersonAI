package com.example.personai.utils

import kotlin.math.sqrt

object VectorUtils {

    // 计算两个向量的余弦相似度
    // 结果范围 -1 到 1，越接近 1 越相似
    fun cosineSimilarity(v1: List<Float>, v2: List<Float>): Double {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0.0

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }

        if (normA == 0.0 || normB == 0.0) return 0.0
        return dotProduct / (sqrt(normA) * sqrt(normB))
    }

    // 计算多个向量的平均向量
    fun averageVector(vectors: List<List<Float>>): List<Float> {
        if (vectors.isEmpty()) return emptyList()
        val dimension = vectors[0].size
        val result = MutableList(dimension) { 0.0f }

        for (vector in vectors) {
            if (vector.size != dimension) continue
            for (i in vector.indices) {
                result[i] += vector[i]
            }
        }

        return result.map { it / vectors.size }
    }
}