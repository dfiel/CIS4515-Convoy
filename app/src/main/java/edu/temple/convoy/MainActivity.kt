package edu.temple.convoy

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject


class MainActivity : AppCompatActivity(), DashboardFragment.DashboardInterface {

    var serviceIntent: Intent? = null
    val convoyViewModel : ConvoyViewModel by lazy {
        ViewModelProvider(this).get(ConvoyViewModel::class.java)
    }

    // Update ViewModel with location data whenever received from LocationService
    var locationHandler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            convoyViewModel.setLocation(msg.obj as LatLng)
        }
    }

    var serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {

            // Provide service with handler
            (iBinder as LocationService.LocationBinder).setHandler(locationHandler)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()
        serviceIntent = Intent(this, LocationService::class.java)

        convoyViewModel.getConvoyId().observe(this) {
            if (!it.isNullOrEmpty())
                supportActionBar?.title = "Convoy - $it"
            else
                supportActionBar?.title = "Convoy"
        }

        Helper.user.getConvoyId(this)?.run {
            convoyViewModel.setConvoyId(this)
            startLocationService()
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1
            )
        }

    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel("default", "Active Convoy", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun createConvoy() {
        Helper.api.createConvoy(this, Helper.user.get(this), Helper.user.getSessionKey(this)!!, object: Helper.api.Response {
            override fun processResponse(response: JSONObject) {
                if (Helper.api.isSuccess(response)) {
                    convoyViewModel.setConvoyId(response.getString("convoy_id"))
                    Helper.user.saveConvoyId(this@MainActivity, convoyViewModel.getConvoyId().value!!)
                    startLocationService()
                } else {
                    Toast.makeText(this@MainActivity, Helper.api.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
            }

        })
    }

    override fun endConvoy() {
        AlertDialog.Builder(this).setTitle("Close Convoy")
            .setMessage("Are you sure you want to close the convoy?")
            .setPositiveButton("Yes"
            ) { _, _ -> Helper.api.closeConvoy(
                this,
                Helper.user.get(this),
                Helper.user.getSessionKey(this)!!,
                convoyViewModel.getConvoyId().value!!,
                object: Helper.api.Response {
                    override fun processResponse(response: JSONObject) {
                        if (Helper.api.isSuccess(response)) {
                            convoyViewModel.setConvoyId("")
                            Helper.user.clearConvoyId(this@MainActivity)
                            stopLocationService()
                        } else
                            Toast.makeText(this@MainActivity, Helper.api.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                    }

                }
            )}
            .setNegativeButton("Cancel") { p0, _ -> p0.cancel() }
            .show()
    }

    override fun logout() {
        Helper.user.clearSessionData(this)
        Navigation.findNavController(findViewById(R.id.fragmentContainerView))
            .navigate(R.id.action_dashboardFragment_to_loginFragment)
    }

    private fun startLocationService() {
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        startService(serviceIntent)
    }
    private fun stopLocationService() {
        unbindService(serviceConnection)
        stopService(serviceIntent)
    }
}