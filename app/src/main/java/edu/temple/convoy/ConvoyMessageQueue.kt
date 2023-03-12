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

class ConvoyMessageQueue(private val context: Context, private val scope: CoroutineScope,
                         private val viewModel: ConvoyViewModel) {
    data class ConvoyMessage(val username: String, val file: File)

    private var mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .build()
        )
        setOnCompletionListener { cleanupMessage(); queueMessage() }
        setOnPreparedListener { startMessage() }
    }
    private var queue = ConcurrentLinkedQueue<ConvoyMessage>()

    fun add(element: ConvoyMessage?) {
        queue.add(element)
        if (!mediaPlayer.isPlaying) {
            queueMessage()
        }
    }

    fun downloadMessage(messageURL: URL, username: String) {
        scope.launch(Dispatchers.IO) {
            val recordingFile = File(
                context.filesDir.absolutePath + File.separator + username + '-' +
                        System.nanoTime() + ".rec.m4a"
            )
            recordingFile.writeBytes(messageURL.readBytes())
            this@ConvoyMessageQueue.add(ConvoyMessage(username, recordingFile))
        }
    }

    private fun cleanupMessage() {
        val message = queue.poll()
        if (queue.size == 0) {
            viewModel.postConvoyMessage(null)
        }
        scope.launch(Dispatchers.IO) {
            message!!.file.delete()
        }
    }

    private fun queueMessage() {
        mediaPlayer.reset()
        if (queue.size == 0) {
            return
        }
        val message = queue.peek()
        mediaPlayer.apply {
            setDataSource(context, Uri.fromFile(message!!.file))
            prepareAsync()
        }
    }

    private fun startMessage() {
        mediaPlayer.start()
        viewModel.postConvoyMessage(queue.peek()!!)
    }
}