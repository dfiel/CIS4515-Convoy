package edu.temple.convoy

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.piasy.rxandroidaudio.AudioRecorder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import java.io.File

class DashboardFragment : Fragment() {

    private val convoyViewModel: ConvoyViewModel by lazy {
        ViewModelProvider(requireActivity()).get(ConvoyViewModel::class.java)
    }

    lateinit var mainFAB: FloatingActionButton
    lateinit var startFAB: FloatingActionButton
    lateinit var joinFAB: FloatingActionButton
    lateinit var micFAB: FloatingActionButton
    lateinit var txtStart: TextView
    lateinit var txtJoin: TextView
    lateinit var txtMessage: TextView
    private var fabsVisible = false
    var recordingFile: File? = null

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

        val layout = inflater.inflate(R.layout.fragment_dashboard, container, false)

        mainFAB = layout.findViewById(R.id.mainFAB)
        startFAB = layout.findViewById(R.id.startFAB)
        joinFAB = layout.findViewById(R.id.joinFAB)
        micFAB = layout.findViewById(R.id.micFAB)
        txtJoin = layout.findViewById(R.id.txtJoin)
        txtStart = layout.findViewById(R.id.txtStart)
        txtMessage = layout.findViewById(R.id.txtMessage)

        // Query the server for the current Convoy ID (if available)
        // and use it to close the convoy
        mainFAB.setOnLongClickListener {
            convoyViewModel.setConvoyId("")
            Helper.user.clearConvoyId(requireContext())
            if (Helper.user.getSessionKey(requireContext())
                    .isNullOrEmpty()
            ) return@setOnLongClickListener true
            Helper.api.queryStatus(requireContext(),
                Helper.user.get(requireContext()),
                Helper.user.getSessionKey(requireContext())!!,
                object : Helper.api.Response {
                    override fun processResponse(response: JSONObject) {
                        if (Helper.api.isSuccess(response)) {
                            Helper.api.endConvoy(
                                requireContext(),
                                Helper.user.get(requireContext()),
                                Helper.user.getSessionKey(requireContext())!!,
                                response.getString("convoy_id"),
                                null
                            )
                        }
                    }
                })
            true
        }

        mainFAB.setOnClickListener { toggleFABs() }
        startFAB.setOnClickListener {
            toggleFABs()
            (activity as DashboardInterface).createConvoy()
            Helper.user.saveStartedConvoy(requireContext(), true)
        }
        joinFAB.setOnClickListener {
            toggleFABs()
            (activity as DashboardInterface).joinConvoy()
            Helper.user.saveStartedConvoy(requireContext(), false)
        }
        micFAB.setOnClickListener {
            if (requireContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return@setOnClickListener
            }
            if (recordingFile == null) {
                recordingFile = File(
                    requireContext().filesDir.absolutePath + File.separator +
                            System.nanoTime() + ".rec.m4a"
                )
                AudioRecorder.getInstance().apply {
                    prepareRecord(
                        MediaRecorder.AudioSource.MIC,
                        MediaRecorder.OutputFormat.MPEG_4,
                        MediaRecorder.AudioEncoder.AAC,
                        recordingFile
                    )
                    startRecord()
                }
                micFAB.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#e91e63"))
            } else {
                AudioRecorder.getInstance().stopRecord()
                (activity as DashboardInterface).messageConvoy(recordingFile!!)
                micFAB.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#03DAC5"))
                recordingFile = null
            }
        }

        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Use ViewModel to determine if we're in an active Convoy
        // Change FloatingActionButton behavior depending on if we're
        // currently in a convoy
        convoyViewModel.getConvoyId().observe(requireActivity()) {
            if (it.isNullOrEmpty()) {
                mainFAB.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#03DAC5"))
                mainFAB.setImageResource(R.drawable.add_24)
                mainFAB.setOnClickListener { toggleFABs() }
                micFAB.visibility = View.GONE
            } else {
                mainFAB.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#e91e63"))
                mainFAB.setImageResource(R.drawable.close_24)
                mainFAB.setOnClickListener {
                    if (Helper.user.getStartedConvoy(requireContext())) (activity as DashboardInterface).endConvoy()
                    else (activity as DashboardInterface).leaveConvoy()
                }
                micFAB.visibility = View.VISIBLE
            }

        }

        convoyViewModel.getConvoyMessage().observe(requireActivity()) {
            if (it == null) {
                txtMessage.visibility = View.GONE
            }
            else {
                txtMessage.text = getString(R.string.broadcast_message, it.username)
                txtMessage.visibility = View.VISIBLE
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
        } else {
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
        fun messageConvoy(file: File)
        fun logout()
    }

}