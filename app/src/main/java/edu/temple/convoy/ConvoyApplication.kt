package edu.temple.convoy

import android.app.Application
import org.json.JSONObject

class ConvoyApplication : Application() {

    var messageCallback: FCMCallback? = null
    interface FCMCallback {
        fun messageReceive(message: JSONObject)
    }

    fun registerFCMCallback(callback: FCMCallback?) {
        messageCallback = callback
    }

    fun getFCMCallback() : FCMCallback? {
        return messageCallback
    }
}