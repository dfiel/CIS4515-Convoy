package edu.temple.convoy

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

class ConvoyMessageQueue(val context: Context) : ConcurrentLinkedQueue<ConvoyMessageQueue.ConvoyMessage>() {
    data class ConvoyMessage(val username: String, val file: File)

    private var mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .build()
        )
        setOnCompletionListener { reset(); queueMessage() }
        setOnPreparedListener { start() }
    }

    fun downloadMessage(scope: CoroutineScope, baseDir: File, messageURL: URL, username: String) {
        scope.launch(Dispatchers.IO) {
            val recordingFile = File(
                baseDir.absolutePath + File.separator + username +
                        '-' + System.nanoTime() + ".rec.m4a"
            )
            recordingFile.writeBytes(messageURL.readBytes())
            this@ConvoyMessageQueue.add(ConvoyMessage(username, recordingFile))
        }
    }

    private fun queueMessage() {
        if (this.size == 0) { return }
        val message = this.poll()
        mediaPlayer.apply {
            setDataSource(context, Uri.fromFile(message!!.file))
            setVolume(1f,1f)
            prepareAsync()
        }
    }

    override fun add(element: ConvoyMessage?): Boolean {
        super.add(element)
        if (!mediaPlayer.isPlaying) {
            queueMessage()
        }
        return true
    }
}