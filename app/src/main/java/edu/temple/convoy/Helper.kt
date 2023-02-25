package edu.temple.convoy

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
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

        fun createConvoy(context: Context, user: User, sessionKey: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "CREATE"),
                Pair("username", user.username),
                Pair("session_key", sessionKey)
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun closeConvoy(context: Context, user: User, sessionKey: String, convoyId: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "CLOSE"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("convoy_id", convoyId)
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

        private fun makeRequest(context: Context, endPoint: String, params: MutableMap<String, String>, responseCallback: Response?) {
            Volley.newRequestQueue(context)
                .add(object: StringRequest(Request.Method.POST, API_BASE + endPoint, {
                    Log.d("Server Response", it)
                    responseCallback?.processResponse(JSONObject(it))
                }, {}){
                    override fun getParams(): MutableMap<String, String> {
                            return params;
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