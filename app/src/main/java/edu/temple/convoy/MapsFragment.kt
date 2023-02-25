package edu.temple.convoy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions


class MapsFragment : Fragment() {

    lateinit var map: GoogleMap
    var myMarker: Marker? = null
    val convoyUsers = mutableMapOf<String, Marker?>()

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
    }

    val convoyViewModel : ConvoyViewModel by lazy {
        ViewModelProvider(this).get(ConvoyViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)


        // Update location on map whenever ViewModel is updated
        convoyViewModel.getLocation().observe(requireActivity()) {
                if (myMarker == null) myMarker = map.addMarker(
                    MarkerOptions().position(it)
                ) else myMarker?.position = it
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 17f))
            }

        convoyViewModel.getConvoyUsers().observe(requireActivity()) { users ->
            val localUser = Helper.user.get(requireActivity())
            val userSet = mutableSetOf<String>()
            val mapBounds = LatLngBounds.Builder()
            for(i in 0 until users.length()) {
                val user = users.getJSONObject(i)
                val username = user.getString("username")
                val latLng = LatLng(user.getDouble("latitude"), user.getDouble("longitude"))
                if (username == localUser.username) continue // Filter out own location from convoy updates
                userSet.add(username)
                mapBounds.include(latLng)
                if (convoyUsers.containsKey(username)) {
                    convoyUsers[username]?.position  = latLng
                }
                else {
                    convoyUsers[username] = map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(username)
                            .snippet("${user.getString("firstname")} ${user.getString("lastname")}")
                            .icon(BitmapDescriptorFactory.defaultMarker(i * 18f % 360)) // Different color per convoy user
                    )
                }
            }
            convoyUsers.keys.subtract(userSet).forEach { oldUser ->
                convoyUsers[oldUser]?.remove()
                convoyUsers.remove(oldUser)
            }
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(mapBounds.build(), 5))
        }
    }
}