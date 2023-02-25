package edu.temple.convoy

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

class LocationService : Service() {

    var locationManager: LocationManager? = null
    var locationListener: LocationListener? = null
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var notification: Notification? = null
    var handler: Handler? = null

    // Define a binder to accept a handler
    inner class LocationBinder : Binder() {
        fun setHandler(handler: Handler) {
            this@LocationService.handler = handler
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return LocationBinder()
    }

    override fun onCreate() {
        super.onCreate()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                Log.d("LocationService", "Location update received")
                val location = p0.locations.first()
                if (handler != null) {
                    val msg: Message = Message.obtain()
                    msg.obj = LatLng(location.latitude, location.longitude)
                    handler!!.sendMessage(msg)
                }
            }
        }
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(10f)
            .build()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Fetch location manager and define location listener to report
        // user location updates to connected client
        locationManager = getSystemService(LocationManager::class.java)
        locationListener = LocationListener { location: Location ->
            if (handler != null) {
                val msg: Message = Message.obtain()
                msg.obj = LatLng(location.latitude, location.longitude)
                handler!!.sendMessage(msg)
            }
        }

        // Notification for Foreground Service
        notification = NotificationCompat.Builder(this)
            .setContentTitle("Group active")
            .setContentText("Currently in group []")
            .setSmallIcon(R.drawable.ic_menu_directions)
            .setChannelId("default")
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Start requesting location updates when service Started
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startForeground(1, notification);
            Log.d("Location Service", "Started")
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

//            locationListener?.apply {
//                locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, this)
//            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        handler = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(locationListener!!)
        if (this::fusedLocationClient.isInitialized && this::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}