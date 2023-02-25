package edu.temple.convoy

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "New FCM Token: $token")
        Helper.user.setTokenRegistrationStatus(this, false)
        if (!Helper.user.getSessionKey(this).isNullOrEmpty()) {
            Helper.api.registerToken(
                this,
                Helper.user.get(this),
                Helper.user.getSessionKey(this)!!,
                token,
                object : Helper.api.Response {
                    override fun processResponse(response: JSONObject) {
                        if (Helper.api.isSuccess(response)) {
                            Helper.user.setTokenRegistrationStatus(this@FCMService, true)
                        }
                    }
                }
            )
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data["payload"].toString()

        (application as ConvoyApplication).messageCallback?.run { messageReceive(JSONObject(data)) }
    }
}