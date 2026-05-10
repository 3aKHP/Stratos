package com.gpsplane.app.data.track

import com.gpsplane.app.data.FlightPhase
import com.gpsplane.app.data.model.GpsData
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Owns the lifecycle of one active GPX track file. Callers feed GPS
 * samples via [onGpsSample] and the current [FlightPhase]; the recorder
 * opens a file when the phase first goes [FlightPhase.AIRBORNE] and
 * closes it on the transition back to [FlightPhase.GROUND].
 *
 * File naming: `tracks/YYYYMMDD-HHMMSS.gpx` in the provided [root]
 * directory (expected to be `filesDir/tracks` — Auto-Backup eligible,
 * which is fine for passenger flights where files stay < 1 MB each).
 *
 * **Not thread-safe.** The owning coroutine must serialise all access
 * — `onGpsSample`, the [enabled] setter, and [close]. The service
 * funnels both the GPS stream and the recording-enabled toggle onto
 * its collector coroutine for exactly this reason.
 *
 * Recording can be disabled with [enabled] = false without disturbing
 * the rest of the service; a false→true transition mid-flight starts a
 * new file at the next sample.
 */
class TrackRecorder(
    private val root: File,
    private val creator: String,
    private val writerFactory: (File) -> Writer = { file ->
        file.parentFile?.mkdirs()
        BufferedWriter(FileWriter(file))
    },
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    data class State(
        val recording: Boolean,
        val currentFile: File?,
        val pointsWritten: Int,
    ) {
        companion object {
            val IDLE = State(recording = false, currentFile = null, pointsWritten = 0)
        }
    }

    private var writer: Writer? = null
    private var currentFile: File? = null
    private var pointsWritten = 0
    private var lastPhase: FlightPhase = FlightPhase.GROUND
    var enabled: Boolean = true
        set(value) {
            field = value
            if (!value) stopInternal()
        }

    fun state(): State = State(
        recording = writer != null,
        currentFile = currentFile,
        pointsWritten = pointsWritten,
    )

    /**
     * Feed one GPS sample + current flight phase. Handles start/stop on
     * phase transitions and writes a point when a file is open.
     */
    fun onGpsSample(gps: GpsData, phase: FlightPhase) {
        val prevPhase = lastPhase
        lastPhase = phase

        if (prevPhase == FlightPhase.AIRBORNE && phase == FlightPhase.GROUND) {
            stopInternal()
            return
        }

        if (!enabled) return
        if (!gps.hasFix) return
        if (phase != FlightPhase.AIRBORNE) return

        if (writer == null) start()

        writer?.let { w ->
            GpxWriter.writePoint(w, gps)
            pointsWritten++
        }
    }

    private fun start() {
        if (writer != null) return
        val filename = FILE_NAME_FORMAT.get()!!.format(Date(clock()))
        val file = File(root, "$filename.gpx")
        currentFile = file
        pointsWritten = 0
        val w = writerFactory(file)
        GpxWriter.writeHeader(w, creator, filename, clock())
        writer = w
    }

    private fun stopInternal() {
        val w = writer ?: return
        try {
            GpxWriter.writeFooter(w)
        } finally {
            runCatching { w.close() }
            writer = null
        }
    }

    /** Flushes in-memory bytes to disk without closing the current file. */
    fun flush() {
        runCatching { writer?.flush() }
    }

    /** Closes any open file. Intended for service teardown. */
    fun close() = stopInternal()

    companion object {
        private val FILE_NAME_FORMAT = ThreadLocal.withInitial {
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
    }
}
