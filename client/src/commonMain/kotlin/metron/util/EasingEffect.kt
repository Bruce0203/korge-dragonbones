package metron.util

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.event.*
import korlibs.io.lang.*
import korlibs.korge.view.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlinx.uuid.*
import metron.*
import metron.util.Effect.Companion.EffectComponentKey
import util.*

class EasingEffect(
    val easing: Easing,
    val effects: Array<out Effect>,
    val finishing: View.() -> Unit,
    val timeSpan: TimeSpan,
    val view: View
) {
    private lateinit var onDestroyView: Cancellable
    private lateinit var scheduledTask: Cancellable

    fun enableEffect() {
        val span = timeSpan
        startEffect()
        scheduledTask = screen.schedule(span,
            onDelay = { elapsed ->
                val period = elapsed / span
                effects.fastForEach { it.applyEffect(view, easing(period)) }
            }, onStopped = {
                stopEffect()
            }
        )
    }

    fun startEffect() {
        view.parent?.apply {
            if (hasExtra(EffectComponentKey)) {
                getExtraTyped<EasingEffect>(EffectComponentKey)!!.stopEffect()
            }
        }
        val originalParent = view.parent?: return
        val newParent = originalParent.container()
        val easingEffect = this
        newParent.setExtra(EffectComponentKey, easingEffect)

        onDestroyView = newParent.onEvent(DestroyEvent) { newParent.removeFromParent() }
        view.removeFromParent()
        view.addTo(newParent)
    }

    fun stopEffect() {
        val originalParent = view.parent?.parent?: return
        val tuckedParent = view.parent
        val thisEasingEffect = this
        scheduledTask.cancel()
        tuckedParent?.apply {
            if (hasExtra(EffectComponentKey)) {
                if (getExtraTyped<EasingEffect>(EffectComponentKey) == thisEasingEffect) {
                    extra?.remove(EffectComponentKey)
                }
            }
        }
        tuckedParent?.removeFromParent()
        view.removeFromParent()
        view.addTo(originalParent)
        onDestroyView.cancel()
        finishing(view)
    }


}

fun interface Effect {
    fun applyEffect(view: View, value: Float)

    companion object {
        val EffectComponentKey = UUID.generateUUID().toString()
        fun View.easingEffect(
            timeSpan: TimeSpan,
            easing: Easing = Easing.EASE_OUT,
            effects: Array<Effect>,
            finishing: View.() -> Unit = {},
        ) = EasingEffect(
            easing = easing,
            finishing = finishing,
            timeSpan = timeSpan,
            effects = effects,
            view = this
        ).enableEffect()
        fun posX(magnanimity: Float, isDown: Boolean = false): Effect {
            var origin: Float? = null
            return Effect { view, value ->
                if (origin === null) origin = view.pos.x
                view.positionX(origin!! + magnanimity * if (isDown) 1 - value else value)
            }
        }
        fun effectPosY(magnanimity: Float, isDown: Boolean = false): Effect {
            var origin: Float? = null
            return Effect { view, value ->
                if (origin === null) origin = view.pos.y
                view.positionY(origin!! + magnanimity * if (isDown) 1 - value else value)
            }
        }
        fun effectAlpha(magnanimity: Float, isDown: Boolean = false) = Effect { view, value ->
            view.alpha(magnanimity * if (isDown) 1 - value else value)
        }
    }

}

