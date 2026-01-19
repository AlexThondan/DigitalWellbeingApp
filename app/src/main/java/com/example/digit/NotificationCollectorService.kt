package com.example.digit

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Context
import java.util.Calendar

class NotificationCollectorService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val prefs = getSharedPreferences("digit_prefs", Context.MODE_PRIVATE)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        val key = "notif_${packageName}_$today"
        val currentCount = prefs.getInt(key, 0)

        prefs.edit().putInt(key, currentCount + 1).apply()
    }
}