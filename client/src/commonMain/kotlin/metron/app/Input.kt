package metron.app

import event.*
import korlibs.event.*
import korlibs.korge.input.*
import korlibs.time.*
import metron.*
import metron.app.systems.AuditSpawnerSystem.Companion.audit

fun Stage.enableInput() = screen.apply {
    onEvent(HitEvent) {
        if (elapsedSeconds < 0.seconds) return@onEvent
        audit(it.delta)
    }
    keys {
        down(Key.SPACE) { hit(it.deltaTime) }
        down(Key.ESCAPE) {
            isPausedByUser = !isPausedByUser
            if (isPaused) {
                playingMusic?.pause()
            } else {
                playingMusic?.resume()
            }
        }
    }
    onEvent(TouchEvent.Type.START) { hit(it.currentTime - DateTime.now()) }
}

private fun hit(delta: TimeSpan) {
    screen.dispatch(HitEvent(delta))
}