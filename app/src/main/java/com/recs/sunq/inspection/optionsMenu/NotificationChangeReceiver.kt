package com.recs.sunq.inspection.optionsMenu

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.recs.sunq.inspection.Notification.AppMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (NotificationManager.ACTION_APP_BLOCK_STATE_CHANGED == intent.action) {
            val appMessagingService = AppMessagingService()
            CoroutineScope(Dispatchers.IO).launch {
                appMessagingService.updateFCMToken(context)
            }
        }
    }
}
