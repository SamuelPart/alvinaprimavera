package com.flippy.walkietalkie.audio

import com.flippy.walkietalkie.data.model.VoiceFilter
import kotlin.math.sin
import kotlin.random.Random

object VoiceFilterProcessor {

    fun applyFilter(pcmData: ShortArray, filter: VoiceFilter, sampleRate: Int = 44100): ShortArray {
        return when (filter) {
            VoiceFilter.CLEAN -> pcmData.copyOf()
            VoiceFilter.RADIO -> applyRadioFilter(pcmData, sampleRate)
            VoiceFilter.ROBOT -> applyRobotFilter(pcmData, sampleRate)
            VoiceFilter.DEEP -> applyDeepFilter(pcmData, sampleRate)
            VoiceFilter.HELIUM -> applyHeliumFilter(pcmData, sampleRate)
            VoiceFilter.MEGAPHONE -> applyMegaphoneFilter(pcmData, sampleRate)
            VoiceFilter.ALIEN -> applyAlienFilter(pcmData, sampleRate)
        }
    }

    private fun applyRadioFilter(input: ShortArray, sampleRate: Int): ShortArray {
        val output = ShortArray(input.size)
        var prevLow = 0.0
        val alphaLow = 0.35 // Lowpass cutoff ~3.2kHz
        var prevHighIn = 0.0
        var prevHighOut = 0.0
        val alphaHigh = 0.85 // Highpass cutoff ~350Hz

        for (i in input.indices) {
            val sample = input[i].toDouble() / Short.MAX_VALUE

            // Highpass
            val hp = alphaHigh * (prevHighOut + sample - prevHighIn)
            prevHighIn = sample
            prevHighOut = hp

            // Lowpass
            val lp = prevLow + alphaLow * (hp - prevLow)
            prevLow = lp

            // Soft saturation / Radio tube distortion
            val distorted = Math.tanh(lp * 2.2)

            // Subtle static noise
            val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.02

            val finalSample = ((distorted + noise) * Short.MAX_VALUE * 0.85).toInt()
            output[i] = finalSample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    private fun applyRobotFilter(input: ShortArray, sampleRate: Int): ShortArray {
        val output = ShortArray(input.size)
        val modFreq = 52.0 // 52 Hz ring modulation carrier

        for (i in input.indices) {
            val sample = input[i].toDouble()
            val carrier = sin(2.0 * Math.PI * i / (sampleRate / modFreq))
            val modulated = (sample * carrier).toInt()
            output[i] = modulated.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    private fun applyDeepFilter(input: ShortArray, sampleRate: Int): ShortArray {
        val output = ShortArray(input.size)
        var prev = 0.0
        val alpha = 0.20 // Heavy lowpass

        for (i in input.indices) {
            val sample = input[i].toDouble()
            prev = prev + alpha * (sample - prev)
            val boosted = (prev * 1.4).toInt()
            output[i] = boosted.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    private fun applyHeliumFilter(input: ShortArray, sampleRate: Int): ShortArray {
        val output = ShortArray(input.size)
        var prevIn = 0.0
        var prevOut = 0.0
        val alpha = 0.90 // Heavy highpass

        for (i in input.indices) {
            val sample = input[i].toDouble()
            val hp = alpha * (prevOut + sample - prevIn)
            prevIn = sample
            prevOut = hp

            val boosted = (hp * 1.5).toInt()
            output[i] = boosted.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    private fun applyMegaphoneFilter(input: ShortArray, sampleRate: Int): ShortArray {
        val output = ShortArray(input.size)
        val delaySamples = (0.04 * sampleRate).toInt() // 40ms echo
        val delayBuffer = ShortArray(delaySamples)
        var delayIdx = 0

        for (i in input.indices) {
            val sample = input[i].toDouble() / Short.MAX_VALUE
            val echo = delayBuffer[delayIdx].toDouble() / Short.MAX_VALUE

            // Distortion
            val dist = Math.tanh(sample * 3.0)
            val combined = (dist + echo * 0.4) / 1.4

            val finalSample = (combined * Short.MAX_VALUE).toInt().toShort()
            delayBuffer[delayIdx] = finalSample
            delayIdx = (delayIdx + 1) % delaySamples

            output[i] = finalSample
        }
        return output
    }

    private fun applyAlienFilter(input: ShortArray, sampleRate: Int): ShortArray {
        val output = ShortArray(input.size)
        val tremoloFreq = 18.0

        for (i in input.indices) {
            val sample = input[i].toDouble()
            val trem = 0.5 + 0.5 * sin(2.0 * Math.PI * i / (sampleRate / tremoloFreq))
            val res = (sample * trem).toInt()
            output[i] = res.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }
}
