/*
 * Copyright (C) 2025 ArrowOS-Extended
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.charge

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemProperties
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.android.settings.R

class BypassChargingTileService : TileService() {

    companion object {
        private const val TAG = "BypassChargingTileService"
        private const val PROP_BYPASS_ENABLED = "persist.sys.bypass_charging_enable"
        private const val PROP_BYPASS_SUPPORTED = "persist.sys.battery_bypass_charge_support"
    }

    override fun onStartListening() {
        if (!isBypassSupported() || isGoogleDevice()) {
            Log.e(TAG, "Tile unavailable on Google devices")
            disableAndHideTile(this)
            return
        }

        updateTileState(getCurrentState())
    }
    override fun onClick() {
        if (isGoogleDevice()) {
            Log.e(TAG, "Tile unavailable on Google devices")
            disableAndHideTile(this)
            return
        }

        val newState = !getCurrentState()
        setBypassChargeState(newState)
        updateTileState(newState)
    }

    private fun getCurrentState(): Boolean {
        return SystemProperties.getBoolean(PROP_BYPASS_ENABLED, false)
    }

    private fun setBypassChargeState(enable: Boolean) {
        SystemProperties.set(PROP_BYPASS_ENABLED, if (enable) "true" else "false")
    }

    private fun updateTileState(enabled: Boolean) {
        qsTile?.let {
            it.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            it.label = getString(R.string.bypass_charging_tile_label)
            it.contentDescription = getString(R.string.bypass_charging_tile_description)
            it.updateTile()
        }
    }

    private fun disableAndHideTile(context: Context) {
        qsTile?.let {
            it.state = Tile.STATE_UNAVAILABLE
            it.updateTile()
        }

        val component = ComponentName(context, BypassChargingTileService::class.java)
        context.packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun isGoogleDevice(): Boolean {
        return Build.MANUFACTURER.equals("google", ignoreCase = true)
    }

    private fun isBypassSupported(): Boolean {
        return SystemProperties.getBoolean(PROP_BYPASS_SUPPORTED, false)
    }

    class BootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_BOOT_COMPLETED != intent.action) return

            val isSupported = SystemProperties.getBoolean(PROP_BYPASS_SUPPORTED, false)
            val enableTile = isSupported && !Build.MANUFACTURER.equals("google", ignoreCase = true)
            val component = ComponentName(context, BypassChargingTileService::class.java)

            context.packageManager.setComponentEnabledSetting(
                component,
                if (enableTile)
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}