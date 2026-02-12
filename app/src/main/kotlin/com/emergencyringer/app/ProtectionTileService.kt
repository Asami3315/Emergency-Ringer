package com.emergencyringer.app

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class ProtectionTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        
        // Toggle monitoring state
        val context = applicationContext
        val currentState = EmergencyContactRepository.isMonitoringEnabled(context)
        val newState = !currentState
        
        EmergencyContactRepository.setMonitoringEnabled(context, newState)
        
        // Update tile immediately
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val context = applicationContext
        val isEnabled = EmergencyContactRepository.isMonitoringEnabled(context)
        
        if (isEnabled) {
            // Protection ON state
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Protection ON"
            tile.contentDescription = "Emergency Ringer Protection is ON"
            tile.icon = Icon.createWithResource(context, R.drawable.ic_clock)
        } else {
            // Protection OFF state
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Protection OFF"
            tile.contentDescription = "Emergency Ringer Protection is OFF"
            tile.icon = Icon.createWithResource(context, R.drawable.ic_clock)
        }
        
        tile.updateTile()
    }
    
    companion object {
        fun requestTileUpdate(context: android.content.Context) {
            try {
                TileService.requestListeningState(
                    context,
                    android.content.ComponentName(context, ProtectionTileService::class.java)
                )
            } catch (e: Exception) {
                // Tile not added to Quick Settings yet
            }
        }
    }
}
