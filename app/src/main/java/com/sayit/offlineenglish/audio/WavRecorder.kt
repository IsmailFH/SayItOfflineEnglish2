package com.sayit.offlineenglish.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean

class WavRecorder {
    private val sampleRate = 16000
    private val recording = AtomicBoolean(false)
    private var thread: Thread? = null
    private var audioRecord: AudioRecord? = null

    fun start(output: File) {
        if (recording.get()) return
        output.parentFile?.mkdirs()
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer.coerceAtLeast(4096) * 2,
        )
        audioRecord = recorder
        recording.set(true)
        thread = Thread {
            FileOutputStream(output).use { out ->
                writeWavHeader(out, sampleRate, 1, 16)
                val buffer = ByteArray(minBuffer.coerceAtLeast(4096))
                recorder.startRecording()
                while (recording.get()) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) out.write(buffer, 0, read)
                }
                runCatching { recorder.stop() }
            }
            fixHeader(output)
            recorder.release()
        }.also { it.start() }
    }

    fun stop() {
        recording.set(false)
        thread?.join(1500)
        thread = null
        audioRecord = null
    }

    fun isRecording(): Boolean = recording.get()

    private fun writeWavHeader(out: FileOutputStream, rate: Int, channels: Int, bits: Int) {
        val byteRate = rate * channels * bits / 8
        val header = ByteArray(44)
        "RIFF".toByteArray().copyInto(header, 0)
        "WAVE".toByteArray().copyInto(header, 8)
        "fmt ".toByteArray().copyInto(header, 12)
        putIntLE(header, 16, 16)
        putShortLE(header, 20, 1)
        putShortLE(header, 22, channels)
        putIntLE(header, 24, rate)
        putIntLE(header, 28, byteRate)
        putShortLE(header, 32, channels * bits / 8)
        putShortLE(header, 34, bits)
        "data".toByteArray().copyInto(header, 36)
        out.write(header)
    }

    private fun fixHeader(file: File) {
        val dataSize = (file.length() - 44).coerceAtLeast(0)
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(4); raf.write(intBytesLE((36 + dataSize).toInt()))
            raf.seek(40); raf.write(intBytesLE(dataSize.toInt()))
        }
    }

    private fun putIntLE(a: ByteArray, off: Int, v: Int) { intBytesLE(v).copyInto(a, off) }
    private fun putShortLE(a: ByteArray, off: Int, v: Int) {
        a[off] = (v and 0xff).toByte(); a[off + 1] = ((v shr 8) and 0xff).toByte()
    }
    private fun intBytesLE(v: Int) = byteArrayOf(
        (v and 0xff).toByte(), ((v shr 8) and 0xff).toByte(), ((v shr 16) and 0xff).toByte(), ((v shr 24) and 0xff).toByte()
    )
}
