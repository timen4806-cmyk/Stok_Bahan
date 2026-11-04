package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "sinar_mas_stok_channel"
        const val CHANNEL_NAME = "Pembaruan Stok Sinar Mas"
        const val CHANNEL_DESC = "Notifikasi status stok, pencadangan cloud, dan sistem inventaris"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showNotification(title: String, message: String, notificationId: Int = (System.currentTimeMillis() % 10000).toInt()) {
        if (!hasNotificationPermission()) return

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (_: SecurityException) {
            // Permission denied or missing
        }
    }

    fun notifyStockUpdated(materialName: String, column: String, value: String) {
        showNotification(
            title = "Perubahan Stok Bahan",
            message = "$materialName telah diperbarui pada $column: $value"
        )
    }

    fun notifyCloudBackupSuccess(totalItems: Int) {
        showNotification(
            title = "Pencadangan Cloud Berhasil",
            message = "Sebanyak $totalItems data bahan telah dicadangkan ke Firebase Firestore."
        )
    }

    fun notifyLowStock(materialName: String, remaining: String) {
        showNotification(
            title = "Peringatan Stok Menipis",
            message = "Perhatian: Bahan $materialName tersisa hanya $remaining unit!"
        )
    }
}
