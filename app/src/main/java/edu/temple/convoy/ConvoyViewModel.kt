package edu.temple.convoy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray

// A single View Model is used to store all data we want to retain
// and observe
class ConvoyViewModel : ViewModel() {
    private val location by lazy {
        MutableLiveData<LatLng>()
    }

    private val convoyId by lazy {
        MutableLiveData<String>()
    }

    private val convoyUsers by lazy {
        MutableLiveData<JSONArray>()
    }

    private val convoyMessage by lazy {
        MutableLiveData<ConvoyMessageQueue.ConvoyMessage>()
    }

    fun setConvoyId(id: String) {
        convoyId.value = id
    }

    fun postConvoyId(id: String) {
        convoyId.postValue(id)
    }

    fun setLocation(latLng: LatLng) {
        location.value = latLng
    }

    fun postConvoyUsers(users: JSONArray) {
        convoyUsers.postValue(users)
    }

    fun postConvoyMessage(message: ConvoyMessageQueue.ConvoyMessage?) {
        convoyMessage.postValue(message)
    }

    fun getLocation(): LiveData<LatLng> {
        return location
    }

    fun getConvoyId(): LiveData<String> {
        return convoyId
    }

    fun getConvoyUsers(): LiveData<JSONArray> {
        return convoyUsers
    }

    fun getConvoyMessage(): LiveData<ConvoyMessageQueue.ConvoyMessage> {
        return convoyMessage
    }
}