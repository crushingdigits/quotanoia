package dev.claudewidget.wear

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives usage data pushed from the phone app via Wearable Data Layer.
 * Updates the local cache and requests a tile refresh.
 */
class DataLayerListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            if (event.dataItem.uri.path != PATH_USAGE) continue

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val count = dataMap.getInt("count", 0)

            Log.d(TAG, "Received $count accounts from phone")

            val store = WearApp.instance.store
            store.updateFromDataMap(count) { key ->
                when {
                    key.endsWith("_id") || key.endsWith("_name") || key.endsWith("_error_msg") ->
                        dataMap.getString(key)
                    key.endsWith("_pct") -> dataMap.getFloat(key)
                    key.endsWith("_has_data") || key.endsWith("_has_error") ->
                        dataMap.getBoolean(key)
                    else -> null
                }
            }

            // Request tile update
            try {
                val updater = androidx.wear.tiles.TileService.getUpdater(this)
                updater.requestUpdate(UsageTileService::class.java)
            } catch (e: Exception) {
                Log.w(TAG, "Could not request tile update", e)
            }
        }
    }

    companion object {
        private const val TAG = "DataLayerListener"
        private const val PATH_USAGE = "/claude_usage"
    }
}
