package edu.temple.convoy

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.*
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.util.*


class MainActivity : AppCompatActivity(), DashboardFragment.DashboardInterface,
    ConvoyApplication.FCMCallback {

    var serviceIntent: Intent? = null
    val convoyViewModel: ConvoyViewModel by lazy {
        ViewModelProvider(this).get(ConvoyViewModel::class.java)
    }
    val messageQueue by lazy {
        ConvoyMessageQueue(this)
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

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 2)
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            Log.d("MainActivity", "FCM Token: $it")
            if (!Helper.user.getTokenRegistrationStatus(this) &&
                !Helper.user.getSessionKey(this).isNullOrEmpty()
            ) {
                Helper.api.registerToken(
                    this,
                    Helper.user.get(this),
                    Helper.user.getSessionKey(this)!!,
                    it,
                    object : Helper.api.Response {
                        override fun processResponse(response: JSONObject) {
                            if (Helper.api.isSuccess(response)) {
                                Helper.user.setTokenRegistrationStatus(this@MainActivity, true)
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    Helper.api.getErrorMessage(response),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
            } else {
                Log.d("MainActivity", "Token Already Registered or No Session Key")
            }
        }
        FirebaseMessaging.getInstance().subscribeToTopic("convoyId")
        (application as ConvoyApplication).registerFCMCallback(this)

        convoyViewModel.getLocation().observe(this) {
            if (!convoyViewModel.getConvoyId().value.isNullOrEmpty()) {
                Helper.api.updateConvoy(
                    this,
                    Helper.user.get(this),
                    Helper.user.getSessionKey(this)!!,
                    convoyViewModel.getConvoyId().value!!,
                    it,
                    object : Helper.api.Response {
                        override fun processResponse(response: JSONObject) {
                            if (!Helper.api.isSuccess(response)) {
                                Toast.makeText(
                                    this@MainActivity,
                                    Helper.api.getErrorMessage(response),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
        }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                ?.let {
                    audioManager.setCommunicationDevice(it)
                }
        } else {
            audioManager.isSpeakerphoneOn = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ConvoyApplication).registerFCMCallback(null)
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel("default", "Active Convoy", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun createConvoy() {
        Helper.api.createConvoy(
            this,
            Helper.user.get(this),
            Helper.user.getSessionKey(this)!!,
            object : Helper.api.Response {
                override fun processResponse(response: JSONObject) {
                    if (Helper.api.isSuccess(response)) {
                        convoyViewModel.setConvoyId(response.getString("convoy_id"))
                        Helper.user.saveConvoyId(
                            this@MainActivity,
                            convoyViewModel.getConvoyId().value!!
                        )
                        startLocationService()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            Helper.api.getErrorMessage(response),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            })
    }

    override fun endConvoy() {
        AlertDialog.Builder(this).setTitle("Close Convoy")
            .setMessage("Are you sure you want to close the convoy?")
            .setPositiveButton(
                "Yes"
            ) { _, _ ->
                Helper.api.endConvoy(
                    this,
                    Helper.user.get(this),
                    Helper.user.getSessionKey(this)!!,
                    convoyViewModel.getConvoyId().value!!,
                    object : Helper.api.Response {
                        override fun processResponse(response: JSONObject) {
                            if (!Helper.api.isSuccess(response)) {
                                Toast.makeText(
                                    this@MainActivity,
                                    Helper.api.getErrorMessage(response),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
            .setNegativeButton("Cancel") { p0, _ -> p0.cancel() }
            .show()
    }

    override fun joinConvoy() {
        val convoyEditText = EditText(this)
        AlertDialog.Builder(this).setTitle("Join Convoy")
            .setView(convoyEditText)
            .setPositiveButton("Join") { _, _ ->
                Helper.api.joinConvoy(
                    this,
                    Helper.user.get(this),
                    Helper.user.getSessionKey(this)!!,
                    convoyEditText.text.toString(),
                    object : Helper.api.Response {
                        override fun processResponse(response: JSONObject) {
                            if (Helper.api.isSuccess(response)) {
                                convoyViewModel.setConvoyId(response.getString("convoy_id"))
                                Helper.user.saveConvoyId(
                                    this@MainActivity,
                                    convoyViewModel.getConvoyId().value!!
                                )
                                startLocationService()
                            } else
                                Toast.makeText(
                                    this@MainActivity,
                                    Helper.api.getErrorMessage(response),
                                    Toast.LENGTH_SHORT
                                ).show()
                        }
                    }
                )
            }
            .setNegativeButton("Cancel") { p0, _ -> p0.cancel() }
            .show()
    }

    override fun leaveConvoy() {
        AlertDialog.Builder(this).setTitle("Leave Convoy")
            .setMessage("Are you sure you want to leave the convoy?")
            .setPositiveButton(
                "Yes"
            ) { _, _ ->
                Helper.api.leaveConvoy(
                    this,
                    Helper.user.get(this),
                    Helper.user.getSessionKey(this)!!,
                    convoyViewModel.getConvoyId().value!!,
                    object : Helper.api.Response {
                        override fun processResponse(response: JSONObject) {
                            if (Helper.api.isSuccess(response)) {
                                convoyViewModel.setConvoyId("")
                                Helper.user.clearConvoyId(this@MainActivity)
                                stopLocationService()
                            } else
                                Toast.makeText(
                                    this@MainActivity,
                                    Helper.api.getErrorMessage(response),
                                    Toast.LENGTH_SHORT
                                ).show()
                        }

                    }
                )
            }
            .setNegativeButton("Cancel") { p0, _ ->
                p0.cancel()

            }
            .show()
    }

    override fun messageConvoy(file: File) {
        lifecycleScope.launch(Dispatchers.IO) {
            Helper.api.messageConvoy(
                this@MainActivity,
                Helper.user.get(this@MainActivity),
                Helper.user.getSessionKey(this@MainActivity)!!,
                convoyViewModel.getConvoyId().value!!,
                file.readBytes(),
                object : Helper.api.Response {
                    override fun processResponse(response: JSONObject) {
                        if (!Helper.api.isSuccess(response)) {
                            Toast.makeText(
                                this@MainActivity,
                                Helper.api.getErrorMessage(response),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        file.delete()
                    }
                }
            )
        }
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

    @SuppressLint("CheckResult")
    override fun messageReceive(message: JSONObject) {
        Log.d("MainActivity", "FCM Message Received: $message")
        val action = message.getString("action")
        if (action == "END") {
            convoyViewModel.postConvoyId("")
            Helper.user.clearConvoyId(this)
            stopLocationService()
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@MainActivity, "Convoy Ended", Toast.LENGTH_SHORT).show()
            }
        }
        if (action == "UPDATE") {
            convoyViewModel.postConvoyUsers(message.getJSONArray("data"))
        }
        if (action == "MESSAGE") {
            val username = message.getString("username")
            if (username == Helper.user.get(this).username) {
                return
            }
            val fileURL = URL(
                message.getString("message_file")
                    .replace("http://", "https://")
            )
            messageQueue.downloadMessage(lifecycleScope, filesDir, fileURL, username)
        }
    }
}