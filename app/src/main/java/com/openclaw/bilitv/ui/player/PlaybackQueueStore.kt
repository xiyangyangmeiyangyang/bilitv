package com.openclaw.bilitv.ui.player

object PlaybackQueueStore {
    @Volatile
    private var queue: List<String> = emptyList()

    @Volatile
    private var currentIndex: Int = -1

    @Synchronized
    fun setQueue(videoIds: List<String>, currentId: String) {
        val normalized = videoIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (normalized.isEmpty()) {
            queue = if (currentId.isBlank()) emptyList() else listOf(currentId)
            currentIndex = if (queue.isEmpty()) -1 else 0
            return
        }
        queue = if (normalized.contains(currentId)) {
            normalized
        } else {
            listOf(currentId) + normalized
        }.distinct()
        currentIndex = queue.indexOf(currentId).takeIf { it >= 0 } ?: 0
    }

    @Synchronized
    fun ensureCurrent(currentId: String) {
        if (currentId.isBlank()) return
        if (queue.isEmpty()) {
            queue = listOf(currentId)
            currentIndex = 0
            return
        }
        val index = queue.indexOf(currentId)
        if (index >= 0) {
            currentIndex = index
        } else {
            queue = queue + currentId
            currentIndex = queue.lastIndex
        }
    }

    @Synchronized
    fun next(currentId: String): String? {
        ensureCurrent(currentId)
        if (currentIndex < 0) return null
        val target = queue.getOrNull(currentIndex + 1) ?: return null
        currentIndex += 1
        return target
    }

    @Synchronized
    fun previous(currentId: String): String? {
        ensureCurrent(currentId)
        if (currentIndex <= 0) return null
        currentIndex -= 1
        return queue.getOrNull(currentIndex)
    }
}
