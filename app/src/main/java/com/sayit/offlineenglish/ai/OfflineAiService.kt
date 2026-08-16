package com.sayit.offlineenglish.ai

import android.content.Context
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class OfflineAiService(private val context: Context) {
    private val modelDir: File
        get() = (context.getExternalFilesDir("models") ?: File(context.filesDir, "models")).also { it.mkdirs() }

    val speechModelFile: File get() = File(modelDir, "speech-model.bin")

    fun status(): ModelStatus = ModelStatus(
        textReady = true, // Writing/grammar engine is built into the app and is instant.
        speechReady = speechModelFile.exists() && speechModelFile.length() > 50_000_000,
    )

    suspend fun downloadMissingModels(onProgress: (label: String, percent: Int) -> Unit) = withContext(Dispatchers.IO) {
        if (!status().speechReady) {
            downloadFile(SPEECH_MODEL_URL, speechModelFile, "موديل الصوت", onProgress)
        }
        onProgress("جاهز", 100)
    }

    suspend fun improveSentence(sentence: String): ImproveFeedback = withContext(Dispatchers.Default) {
        LocalEnglishCoach.improve(sentence)
    }

    suspend fun reviewWriting(text: String): WritingFeedback = withContext(Dispatchers.Default) {
        LocalEnglishCoach.review(text)
    }

    suspend fun transcribe(audioFile: File): String = withContext(Dispatchers.IO) {
        if (!speechModelFile.exists()) return@withContext ""
        try {
            val model = Whisper.loadModel(context, speechModelFile.absolutePath)
            try {
                Whisper.transcribe(model, audioFile.absolutePath, WhisperConfig(language = "en")).text.trim()
            } finally {
                Whisper.releaseModel(model)
            }
        } catch (_: Throwable) {
            ""
        }
    }

    private fun downloadFile(
        sourceUrl: String,
        destination: File,
        label: String,
        onProgress: (String, Int) -> Unit,
    ) {
        destination.parentFile?.mkdirs()
        val partial = File(destination.absolutePath + ".part")
        if (partial.exists()) partial.delete()

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 20_000
                readTimeout = 45_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SayIt-Offline-English/0.2")
            }
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Download failed: HTTP ${connection.responseCode}")
            }

            val total = connection.contentLengthLong.coerceAtLeast(1L)
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
                    var downloaded = 0L
                    var lastPercent = -1
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        val percent = ((downloaded * 100L) / total).toInt().coerceIn(0, 99)
                        if (percent != lastPercent) {
                            onProgress(label, percent)
                            lastPercent = percent
                        }
                    }
                    output.fd.sync()
                }
            }

            if (partial.length() < 10_000_000) {
                throw IllegalStateException("Downloaded model file is unexpectedly small")
            }
            if (destination.exists()) destination.delete()
            if (!partial.renameTo(destination)) {
                partial.copyTo(destination, overwrite = true)
                partial.delete()
            }
            onProgress(label, 100)
        } catch (t: Throwable) {
            partial.delete()
            throw t
        } finally {
            connection?.disconnect()
        }
    }

    fun release() = Unit

    companion object {
        private const val SPEECH_MODEL_URL =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin?download=true"
    }
}

data class ModelStatus(val textReady: Boolean, val speechReady: Boolean)
