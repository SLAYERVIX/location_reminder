package com.udacity.project4.utils

import android.os.Build
import com.google.android.gms.location.Geofence

object Constants {
    val FINE_LOCATION=android.Manifest.permission.ACCESS_FINE_LOCATION
    val COARSE_LOCATION=android.Manifest.permission.ACCESS_COARSE_LOCATION
    val BACKGROUND_LOCATION=android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
    val RUNNING_Q_OR_LATER= Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val RUNNING_S_OR_LATER= Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val REQUEST_LOCATION_PERMISSION_CODE=1
    val REQUEST_BACKGROUND_LOCATION_PERMISSION_CODE=2
    val REQUEST_TURN_ON_GPS_CODE=2
    val MIN_TIME_UPDATES = 50000L
    val MIN_DISTANCE_UPDATES = 10f
    val GEOFENCE_RADIUS: Float = 500f
    val GEOFENCE_TRANSITION_TYPES = Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
}