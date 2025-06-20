package com.aryoucovered.app.core.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context.NOTIFICATION_SERVICE
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.aryoucovered.app.R
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    private lateinit var notificationChannel: NotificationChannel
    private lateinit var notificationManager: NotificationManager
    // ...
    override fun onReceive(context: Context?, intent: Intent) {
        notificationChannel = NotificationChannel("GeofenceChannel", "GeofenceChannel", NotificationManager.IMPORTANCE_HIGH)
        notificationManager =
            context?.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

        val geofencingEvent = GeofencingEvent.fromIntent(intent)?: return
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceBroadcastReceiver", errorMessage)
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            // Get the transition details as a String.
            val geofenceTransitionDetails = getGeofenceTransitionDetails(
                this,
                geofenceTransition,
                triggeringGeofences!!
            )

            // Send notification and log the transition details.
            sendNotification(context, geofenceTransitionDetails)
            Log.i("GeofenceBroadcastReceiver", geofenceTransitionDetails)
        } else {
            // Log the error.
            //val invalidType = getString(R.string.geofence_transition_invalid_type,
            //    geofenceTransition)
            Log.e("GeofenceBroadcastReceiver", "Error occurred while processing geofence transition")
        }
    }

    private fun getGeofenceTransitionDetails(receiver: GeofenceBroadcastReceiver, transition: Int, triggeringGeofences: List<Geofence>): String {
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            return "GameObject nearby!"
        }
        return "No GameOjects nearby, wrong direction!"
    }

    private fun sendNotification(context: Context, geofenceTransitionDetails: String) {
        val notification: Notification = Notification.Builder(context, "GeofenceChannel")
            .setContentTitle("Geofence notification")
            .setContentText("Geofence triggered")
            .setSmallIcon(R.drawable.sf_logo)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notificationManager.notify(1, notification)
        }
    }
}