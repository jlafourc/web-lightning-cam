package com.lightningcam.detector

data class TransientComponent(
    val area: Int,
    val width: Int,
    val height: Int,
) {
    val span: Int get() = maxOf(width, height)
    val elongation: Double get() = span.toDouble() / minOf(width, height)
}

object TransientComponents {
    fun extract(width: Int, height: Int, mask: BooleanArray): List<TransientComponent> {
        require(width > 0 && height > 0)
        require(mask.size == width * height)
        val visited = BooleanArray(mask.size)
        val queue = IntArray(mask.size)
        val components = mutableListOf<TransientComponent>()

        for (start in mask.indices) {
            if (!mask[start] || visited[start]) continue
            var read = 0
            var write = 1
            queue[0] = start
            visited[start] = true
            var area = 0
            var minX = width
            var maxX = 0
            var minY = height
            var maxY = 0

            while (read < write) {
                val index = queue[read++]
                val x = index % width
                val y = index / width
                area++
                minX = minOf(minX, x)
                maxX = maxOf(maxX, x)
                minY = minOf(minY, y)
                maxY = maxOf(maxY, y)

                for (dy in -1..1) for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nextX = x + dx
                    val nextY = y + dy
                    if (nextX !in 0 until width || nextY !in 0 until height) continue
                    val next = nextY * width + nextX
                    if (mask[next] && !visited[next]) {
                        visited[next] = true
                        queue[write++] = next
                    }
                }
            }
            components += TransientComponent(
                area = area,
                width = maxX - minX + 1,
                height = maxY - minY + 1,
            )
        }
        return components
    }
}
