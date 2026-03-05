package com.vectras.vm.setupwizard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.vectras.vm.utils.PermissionUtils

enum class Capability {
    STORAGE_SAF,
    NOTIFICATIONS,
    BATTERY_OPTIMIZATION,
    OVERLAY,
    MEDIA_READ
}

enum class CapabilityState {
    PENDING,
    GRANTED,
    SKIPPED,
    FAILED
}

data class CapabilityItem(
    val capability: Capability,
    val required: Boolean,
    val state: CapabilityState,
    val reason: String?
)

class FirstRunPermissionOrchestrator {

    data class RequestLaunchers(
        val requestStorageSaf: (() -> Unit)? = null,
        val requestNotifications: (() -> Unit)? = null,
        val requestMediaRead: (() -> Unit)? = null,
        val requestBatteryOptimization: (() -> Unit)? = null,
        val requestOverlay: (() -> Unit)? = null
    )

    private var appContext: Context? = null
    private val capabilities = linkedMapOf<Capability, CapabilityItem>()

    fun buildCapabilities(context: Context, vmUiMode: String?): List<CapabilityItem> {
        appContext = context
        capabilities.clear()

        val normalizedMode = vmUiMode?.trim()?.uppercase() ?: ""
        val requiresOverlay = normalizedMode == "X11"

        putCapability(Capability.STORAGE_SAF, required = true)
        putCapability(Capability.NOTIFICATIONS, required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        putCapability(Capability.BATTERY_OPTIMIZATION, required = true)
        putCapability(Capability.MEDIA_READ, required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        putCapability(Capability.OVERLAY, required = requiresOverlay)

        return getUiModel()
    }

    fun evaluate(capability: Capability): CapabilityState {
        val context = appContext ?: return CapabilityState.FAILED
        val current = capabilities[capability] ?: return CapabilityState.FAILED

        if (!current.required) {
            updateState(capability, CapabilityState.SKIPPED, "Not required for this flow")
            return CapabilityState.SKIPPED
        }

        val granted = when (capability) {
            Capability.STORAGE_SAF -> isStorageGranted(context)
            Capability.NOTIFICATIONS -> isNotificationGranted(context)
            Capability.BATTERY_OPTIMIZATION -> isBatteryOptimizationGranted(context)
            Capability.OVERLAY -> isOverlayGranted(context)
            Capability.MEDIA_READ -> isMediaReadGranted(context)
        }

        val state = if (granted) CapabilityState.GRANTED else CapabilityState.PENDING
        val reason = when {
            granted -> null
            else -> "Awaiting capability grant"
        }
        updateState(capability, state, reason)
        return state
    }

    fun request(capability: Capability, activity: Activity, launchers: RequestLaunchers): Boolean {
        val item = capabilities[capability] ?: return false
        if (!item.required) {
            updateState(capability, CapabilityState.SKIPPED, "Not required for this flow")
            return true
        }

        val dispatched = when (capability) {
            Capability.STORAGE_SAF -> {
                launchers.requestStorageSaf?.invoke()
                    ?: run {
                        PermissionUtils.requestStoragePermission(activity)
                        true
                    }
            }

            Capability.NOTIFICATIONS -> {
                launchers.requestNotifications?.invoke()
                    ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
                        true
                    } else {
                        true
                    }
            }

            Capability.BATTERY_OPTIMIZATION -> {
                launchers.requestBatteryOptimization?.invoke()
                    ?: openSettingsIntent(
                        activity,
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${activity.packageName}")
                        }
                    )
            }

            Capability.OVERLAY -> {
                launchers.requestOverlay?.invoke()
                    ?: openSettingsIntent(
                        activity,
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = Uri.parse("package:${activity.packageName}")
                        }
                    )
            }

            Capability.MEDIA_READ -> {
                launchers.requestMediaRead?.invoke()
                    ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        activity.requestPermissions(
                            arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO
                            ),
                            2002
                        )
                        true
                    } else {
                        true
                    }
            }
        }

        if (!dispatched) {
            updateState(capability, CapabilityState.FAILED, "Unable to dispatch capability request")
        }
        return dispatched
    }

    fun isEssentialResolved(): Boolean {
        if (capabilities.isEmpty()) {
            return false
        }
        return capabilities.values
            .filter { it.required }
            .all { it.state == CapabilityState.GRANTED || it.state == CapabilityState.SKIPPED }
    }

    fun getUiModel(): List<CapabilityItem> {
        val snapshot = ArrayList<CapabilityItem>(capabilities.size)
        capabilities.keys.forEach { capability ->
            evaluate(capability)
            capabilities[capability]?.let { snapshot.add(it) }
        }
        return snapshot
    }

    private fun putCapability(capability: Capability, required: Boolean) {
        capabilities[capability] = CapabilityItem(
            capability = capability,
            required = required,
            state = CapabilityState.PENDING,
            reason = null
        )
    }

    private fun updateState(capability: Capability, state: CapabilityState, reason: String?) {
        val previous = capabilities[capability] ?: return
        capabilities[capability] = previous.copy(state = state, reason = reason)
    }

    private fun isStorageGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (context as? Activity)?.let { PermissionUtils.hasPersistedTreePermission(it) } ?: false
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isNotificationGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isBatteryOptimizationGranted(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    private fun isOverlayGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        return Settings.canDrawOverlays(context)
    }

    private fun isMediaReadGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        val imageGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
        val videoGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VIDEO
        ) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        return imageGranted && videoGranted && audioGranted
    }

    private fun openSettingsIntent(activity: Activity, intent: Intent): Boolean {
        return if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
            true
        } else {
            false
        }
    }
}
