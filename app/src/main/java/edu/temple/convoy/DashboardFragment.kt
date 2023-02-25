package edu.temple.convoy

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject

class DashboardFragment : Fragment() {

    lateinit var mainFAB: FloatingActionButton
    lateinit var startFAB: FloatingActionButton
    lateinit var joinFAB: FloatingActionButton
    lateinit var txtStart: TextView
    lateinit var txtJoin: TextView
    var fabsVisible = false
    var startedConvoy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Let the system know that this fragment
        // wants to contribute to the app menu
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val layout =  inflater.inflate(R.layout.fragment_dashboard, container, false)

        mainFAB = layout.findViewById(R.id.mainFAB)
        startFAB = layout.findViewById(R.id.startFAB)
        joinFAB = layout.findViewById(R.id.joinFAB)
        txtJoin = layout.findViewById(R.id.txtJoin)
        txtStart = layout.findViewById(R.id.txtStart)

        // Query the server for the current Convoy ID (if available)
        // and use it to close the convoy
        mainFAB.setOnLongClickListener {
            ViewModelProvider(requireActivity()).get(ConvoyViewModel::class.java).setConvoyId("")
            if (Helper.user.getSessionKey(requireContext()).isNullOrEmpty()) return@setOnLongClickListener true
            Helper.api.queryStatus(requireContext(),
            Helper.user.get(requireContext()),
            Helper.user.getSessionKey(requireContext())!!,
            object: Helper.api.Response {
                override fun processResponse(response: JSONObject) {
                    if (Helper.api.isSuccess(response)) {
                        Helper.api.endConvoy(requireContext(),
                            Helper.user.get(requireContext()),
                            Helper.user.getSessionKey(requireContext())!!,
                            response.getString("convoy_id"),
                            null)
                    }
                }
            })
            true
        }

        mainFAB.setOnClickListener{ toggleFABs() }
        startFAB.setOnClickListener {
            toggleFABs()
            (activity as DashboardInterface).createConvoy()
            startedConvoy = true
        }
        joinFAB.setOnClickListener {
            toggleFABs()
            (activity as DashboardInterface).joinConvoy()
            startedConvoy = false
        }

        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Use ViewModel to determine if we're in an active Convoy
        // Change FloatingActionButton behavior depending on if we're
        // currently in a convoy
        ViewModelProvider(requireActivity()).get(ConvoyViewModel::class.java).getConvoyId().observe(requireActivity()) {
            if (it.isNullOrEmpty()) {
                mainFAB.backgroundTintList  = ColorStateList.valueOf(Color.parseColor("#03DAC5"))
                mainFAB.setImageResource(R.drawable.add_24)
                mainFAB.setOnClickListener { toggleFABs() }
            } else {
                mainFAB.backgroundTintList  = ColorStateList.valueOf(Color.parseColor("#e91e63"))
                mainFAB.setImageResource(R.drawable.close_24)
                mainFAB.setOnClickListener {
                    if (startedConvoy) (activity as DashboardInterface).endConvoy()
                    else (activity as DashboardInterface).leaveConvoy()
                }
            }

        }
    }

    // This fragment places a menu item in the app bar
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.dashboard, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.action_logout) {
            (activity as DashboardInterface).logout()
            return true
        }

        return false
    }

    private fun toggleFABs() {
        if (fabsVisible) {
            startFAB.visibility = View.GONE
            joinFAB.visibility = View.GONE
            txtJoin.visibility = View.GONE
            txtStart.visibility = View.GONE
            fabsVisible = false
            mainFAB.setImageResource(R.drawable.add_24)
        }
        else {
            startFAB.visibility = View.VISIBLE
            joinFAB.visibility = View.VISIBLE
            txtJoin.visibility = View.VISIBLE
            txtStart.visibility = View.VISIBLE
            fabsVisible = true
            mainFAB.setImageResource(R.drawable.close_24)
        }
    }

    interface DashboardInterface {
        fun createConvoy()
        fun endConvoy()
        fun joinConvoy()
        fun leaveConvoy()
        fun logout()
    }

}