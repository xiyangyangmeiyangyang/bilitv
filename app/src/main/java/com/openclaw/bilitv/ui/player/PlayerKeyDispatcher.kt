package com.openclaw.bilitv.ui.player

import android.view.KeyEvent
import java.util.concurrent.atomic.AtomicInteger

object PlayerKeyDispatcher {
    private data class Registration(
        val id: Int,
        val handler: (KeyEvent) -> Boolean
    )

    private val idGenerator = AtomicInteger(1)

    @Volatile
    private var registration: Registration? = null

    fun register(callback: (KeyEvent) -> Boolean): Int {
        val id = idGenerator.getAndIncrement()
        registration = Registration(id = id, handler = callback)
        return id
    }

    fun clear(registrationId: Int? = null) {
        if (registrationId == null) {
            registration = null
            return
        }
        if (registration?.id == registrationId) {
            registration = null
        }
    }

    fun dispatch(event: KeyEvent): Boolean {
        val current = registration ?: return false
        return current.handler(event)
    }
}
