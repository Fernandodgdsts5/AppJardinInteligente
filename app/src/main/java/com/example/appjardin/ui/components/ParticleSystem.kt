package com.example.appjardin.ui.components

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var dx: Float = 0f,
    var dy: Float = 0f,
    var alpha: Float = 1f,
    var scale: Float = 1f,
    var lifetime: Float = 1f,
    var age: Float = 0f,
    var active: Boolean = false,
    var isStar: Boolean = false,
    var isWhite: Boolean = false
) {
    fun reset(startX: Float, startY: Float, targetDx: Float, targetDy: Float, life: Float, star: Boolean, white: Boolean) {
        x = startX
        y = startY
        dx = targetDx
        dy = targetDy
        alpha = 1f
        scale = 1f
        lifetime = life
        age = 0f
        active = true
        isStar = star
        isWhite = white
    }
}

class ParticleSystem(poolSize: Int = 16) {
    val particles: Array<Particle> = Array(poolSize) { Particle() }

    fun emit(startX: Float, startY: Float, count: Int, isBurst: Boolean = false) {
        var emitted = 0
        for (p in particles) {
            if (!p.active) {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = if (isBurst) Random.nextFloat() * 40f + 20f else Random.nextFloat() * 20f + 10f
                val vx = cos(angle) * speed
                val vy = sin(angle) * speed
                val life = Random.nextFloat() * 0.4f + 0.6f // 0.6 to 1.0s
                val isStar = Random.nextBoolean()
                val isWhite = Random.nextFloat() < 0.3f // 30% white
                p.reset(startX, startY, vx, vy, life, isStar, isWhite)
                emitted++
                if (emitted >= count) break
            }
        }
    }

    fun update(dt: Float, maxWidth: Float, maxHeight: Float) {
        for (p in particles) {
            if (p.active) {
                p.age += dt
                if (p.age >= p.lifetime) {
                    p.active = false
                    continue
                }
                p.x += p.dx * dt
                p.y += p.dy * dt
                val progress = p.age / p.lifetime
                p.alpha = (1f - progress).coerceIn(0f, 1f)
                p.scale = 0.5f + progress * 0.8f

                // Clamp to within 12dp bounds around button (~12dp = ~36px depending on density, but let's allow ample range)
                if (p.x < -36f || p.x > maxWidth + 36f || p.y < -36f || p.y > maxHeight + 36f) {
                    p.active = false
                }
            }
        }
    }
}
