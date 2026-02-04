package com.vectras.vm.rafaelia

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * RafaeliaConfig maps UI preferences into QEMU -rafaelia hook arguments.
 */
data class RafaeliaConfig(
    val enabled: Boolean,
    val mode: RafaeliaMode,
    val tickMs: Int,
    val debug: Boolean,
    val autotuneEnabled: Boolean,
    val tcgTbSize: Int,
    val totalMemoryBytes: Long
) {
    fun isValid(): Boolean {
        return mode.id in 0..RafaeliaMode.maxId() && tickMs > 0
    }

    fun sanitized(): RafaeliaConfig {
        val safeMode = RafaeliaMode.fromId(mode.id)
        val safeTick = if (tickMs > 0) tickMs else DEFAULT_TICK_MS
        val safeTbSize = if (tcgTbSize >= MIN_TB_SIZE) tcgTbSize else MIN_TB_SIZE
        return copy(mode = safeMode, tickMs = safeTick, tcgTbSize = safeTbSize)
    }

    @JvmOverloads
    fun clampTcgTbSize(requested: Int = tcgTbSize): Int {
        val base = if (requested >= MIN_TB_SIZE) requested else MIN_TB_SIZE
        return clampByRam(base, totalMemoryBytes)
    }

    fun toQemuArgument(): String? {
        if (!enabled) return null
        val safe = sanitized()
        return "-rafaelia enable=on,mode=${safe.mode.id},tick_ms=${safe.tickMs},debug=${if (safe.debug) 1 else 0}"
    }

    companion object {
        private const val DEFAULT_TICK_MS = 10
        private const val DEFAULT_TB_SIZE = 128
        private const val MIN_TB_SIZE = 64

        @JvmStatic
        fun fromPreferences(context: Context): RafaeliaConfig {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val enabled = prefs.getBoolean(RafaeliaSettings.KEY_RAFAELIA_ENABLED, false)
            val modeId = prefs.getString(RafaeliaSettings.KEY_RAFAELIA_MODE, "0")?.toIntOrNull() ?: 0
            val tickMs = prefs.getString(RafaeliaSettings.KEY_RAFAELIA_TICK_MS, DEFAULT_TICK_MS.toString())
                ?.toIntOrNull() ?: DEFAULT_TICK_MS
            val debug = prefs.getBoolean(RafaeliaSettings.KEY_RAFAELIA_DEBUG, false)
            val autotuneEnabled = prefs.getBoolean(RafaeliaSettings.KEY_RAFAELIA_AUTOTUNE, true)
            val tbSize = prefs.getString(RafaeliaSettings.KEY_RAFAELIA_TCG_TB_SIZE, DEFAULT_TB_SIZE.toString())
                ?.toIntOrNull() ?: DEFAULT_TB_SIZE
            val totalMemoryBytes = com.vectras.vm.utils.DeviceUtils.totalMemoryCapacity(context).toLong()
            return RafaeliaConfig(enabled, RafaeliaMode.fromId(modeId), tickMs, debug, autotuneEnabled, tbSize, totalMemoryBytes)
        }

        private fun clampByRam(tbSize: Int, totalMemoryBytes: Long): Int {
            if (totalMemoryBytes <= 0L) return tbSize
            val totalMiB = (totalMemoryBytes / (1024 * 1024)).toInt()
            val maxByRam = (totalMiB / 16).coerceAtLeast(MIN_TB_SIZE)
            return tbSize.coerceAtMost(maxByRam)
        }
    }
}
