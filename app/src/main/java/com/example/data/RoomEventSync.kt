package com.example.data

import kotlinx.coroutines.CancellationException

suspend fun <T> saveRoomEventAndScheduleSync(
    event: T,
    saveToRoom: suspend (T) -> Unit,
    scheduleSync: () -> Unit
) {
    saveToRoom(event)
    scheduleSync()
}

suspend fun <T> syncRoomEventsAndRemove(
    events: List<T>,
    saveToCloud: suspend (T) -> Unit,
    removeFromRoom: suspend (T) -> Unit
) {
    for (event in events) {
        try {
            saveToCloud(event)
            removeFromRoom(event)
        } catch (e: CancellationException) {
            throw e
        }
        // Other failures propagate to WorkManager. The event remains in Room
        // because it is deleted only after saveToCloud succeeds.
    }
}