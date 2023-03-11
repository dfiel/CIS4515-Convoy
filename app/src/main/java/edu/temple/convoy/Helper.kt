package edu.temple.convoy

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject

/**
 * A helper class to store all functions relating to:
 * API Control
 * User Management
 */
class Helper {

    object api {

        val ENDPOINT_CONVOY = "convoy.php"
        val ENDPOINT_USER = "account.php"

        val API_BASE = "https://kamorris.com/lab/convoy/"

        interface Response {
            fun processResponse(response: JSONObject)
        }

        fun createAccount(context: Context, user: User, password: String, response: Response?){
            val params = mutableMapOf(
                Pair("action", "REGISTER"),
                Pair("username", user.username),
                Pair("password", password),
                Pair("firstname", user.firstname!!),
                Pair("lastname", user.lastname!!)
            )
            makeRequest(context, ENDPOINT_USER, params, response)
        }

        fun login(context: Context, user: User, password: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "LOGIN"),
                Pair("username", user.username),
                Pair("password", password)
            )
            makeRequest(context, ENDPOINT_USER, params, response)
        }

        fun registerToken(context: Context, user: User, sessionKey: String, fcmToken: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "UPDATE"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("fcm_token", fcmToken)
            )
            Log.d("registerToken", params.toString())
            makeRequest(context, ENDPOINT_USER, params, response)
        }

        fun createConvoy(context: Context, user: User, sessionKey: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "CREATE"),
                Pair("username", user.username),
                Pair("session_key", sessionKey)
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun endConvoy(context: Context, user: User, sessionKey: String, convoyId: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "END"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("convoy_id", convoyId)
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun joinConvoy(context: Context, user: User, sessionKey: String, convoyId: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "JOIN"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("convoy_id", convoyId)
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun leaveConvoy(context: Context, user: User, sessionKey: String, convoyId: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "LEAVE"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("convoy_id", convoyId)
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun updateConvoy(context: Context, user: User, sessionKey: String, convoyId: String, location: LatLng, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "UPDATE"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("convoy_id", convoyId),
                Pair("latitude", location.latitude.toString()),
                Pair("longitude", location.longitude.toString())
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun queryStatus(context: Context, user:User, sessionKey: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "QUERY"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun messageConvoy(context: Context, user: User, sessionKey: String, convoyId: String, audioFile: ByteArray, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "Message"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("convoy_id", convoyId)
            )
            val dataPart = mapOf(Pair("message_file", VolleyMultipartRequest.DataPart("message_file", audioFile)))
            multipartRequest(context, ENDPOINT_CONVOY, params, dataPart, response)
        }

        private fun makeRequest(context: Context, endPoint: String, params: MutableMap<String, String>, responseCallback: Response?) {
            Volley.newRequestQueue(context)
                .add(object: StringRequest(Method.POST, API_BASE + endPoint, {
                    Log.d("Server Response", it)
                    responseCallback?.processResponse(JSONObject(it))
                }, {}){
                    override fun getParams(): MutableMap<String, String> {
                            return params
                    }
                })
        }

        private fun multipartRequest(context: Context, endPoint: String, params: MutableMap<String, String>, byteData: Map<String, VolleyMultipartRequest.DataPart>?, responseCallback: Response?) {
            Volley.newRequestQueue(context)
                .add(object: VolleyMultipartRequest(Method.POST, API_BASE+endPoint, {
                    Log.d("Multipart Server Response", it.toString())
                    responseCallback?.processResponse(it);
                }, {}) {
                    override fun getParams(): MutableMap<String, String> {
                        return params
                    }
                    override fun getByteData(): Map<String, DataPart>? {
                        return byteData
                    }
                })
        }

        fun isSuccess(response: JSONObject): Boolean {
            return response.getString("status").equals("SUCCESS")
        }

        fun getErrorMessage(response: JSONObject): String {
            return response.getString("message")
        }

    }

    object user {
        private val SHARED_PREFERENCES_FILE = "shared_prefs"
        private val KEY_SESSION_KEY = "session_key"
        private val KEY_USERNAME = "username"
        private val KEY_FIRSTNAME = "firstname"
        private val KEY_LASTNAME = "lastname"
        private val KEY_CONVOY_ID = "convoy_id"
        private val KEY_TOKEN_REGISTERED = "token_registered"
        private val KEY_STARTED_CONVOY = "started_convoy"

        fun saveSessionData(context: Context, sessionKey: String) {
            getSP(context).edit()
                .putString(KEY_SESSION_KEY, sessionKey)
                .apply()
        }

        fun saveConvoyId(context: Context, groupId: String) {
            getSP(context).edit()
                .putString(KEY_CONVOY_ID, groupId)
                .apply()
        }

        fun saveStartedConvoy(context: Context, state: Boolean) {
            getSP(context).edit()
                .putBoolean(KEY_STARTED_CONVOY, state)
                .apply()
        }

        fun getStartedConvoy(context: Context): Boolean {
            return getSP(context).getBoolean(KEY_STARTED_CONVOY, false)
        }

        fun clearStartedConvoy(context: Context) {
            return getSP(context).edit().remove(KEY_STARTED_CONVOY).apply()
        }

        fun getConvoyId(context: Context): String? {
            return getSP(context).getString(KEY_CONVOY_ID, null)
        }

        fun clearConvoyId(context: Context) {
            getSP(context).edit().remove(KEY_CONVOY_ID)
                .apply()
        }

        fun clearSessionData(context: Context) {
            getSP(context).edit().remove(KEY_SESSION_KEY)
                .apply()
        }

        fun getSessionKey(context: Context): String? {
            return getSP(context).getString(KEY_SESSION_KEY, null)
        }

        fun getTokenRegistrationStatus(context: Context): Boolean {
            return getSP(context).getBoolean(KEY_TOKEN_REGISTERED, false)
        }

        fun setTokenRegistrationStatus(context: Context, status: Boolean) {
            getSP(context).edit().putBoolean(KEY_TOKEN_REGISTERED, status).apply()
        }

        fun saveUser(context: Context, user: User) {
            getSP(context).edit()
                .putString(KEY_USERNAME, user.username)
                .putString(KEY_FIRSTNAME, user.firstname)
                .putString(KEY_LASTNAME, user.lastname)
                .apply()
        }

        fun get(context: Context) : User {
            return User (
                        getSP(context).getString(KEY_USERNAME, "")!!,
                        getSP(context).getString(KEY_FIRSTNAME, ""),
                        getSP(context).getString(KEY_LASTNAME, ""),
                    )
        }
        private fun getSP (context: Context) : SharedPreferences {
            return context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        }
    }


}