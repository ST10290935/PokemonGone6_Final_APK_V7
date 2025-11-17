package com.example.pokemon.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.pokemon.MainActivity
import com.example.pokemon.R

object NotificationHelper {

    private const val CHANNEL_ID_SYNC = "sync_channel"
    private const val CHANNEL_ID_STEPS = "steps_channel"

    private const val NOTIFICATION_ID_SYNC = 1001
    private const val NOTIFICATION_ID_STEPS = 1002

    /**
     * Create notification channels (required for Android 8.0+)
     * Call this once when the app starts
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Sync notifications channel
            val syncChannel = NotificationChannel(
                CHANNEL_ID_SYNC,
                "Data Sync",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for data synchronization status"
            }

            // Step milestone channel
            val stepsChannel = NotificationChannel(
                CHANNEL_ID_STEPS,
                "Step Milestones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for step achievements"
            }

            // Register channels with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(syncChannel)
            notificationManager.createNotificationChannel(stepsChannel)
        }
    }

    /**
     * Show notification when data sync completes
     */
    fun showSyncCompleteNotification(context: Context, itemsSynced: Int) {
        // Intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(R.drawable.pokemon_icon) // You'll need to add this icon
            .setContentTitle("Sync Complete")
            .setContentText("Successfully synced $itemsSynced items to the cloud")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismiss when tapped
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SYNC, notification)
    }

    /**
     * Show notification when user reaches a step milestone
     */
    fun showStepMilestoneNotification(context: Context, steps: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_STEPS)
            .setSmallIcon(R.drawable.pokemon_icon)
            .setContentTitle("Step Milestone Reached! 🎉")
            .setContentText("Great job! You've walked $steps steps today!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibration pattern
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_STEPS, notification)
    }
}