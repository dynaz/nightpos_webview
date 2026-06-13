package com.nightpos.geckoview.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Rolling log of TWA launch attempts written to private app storage.
 * Each entry records the timestamp, target URL, and the Custom Tabs provider
 * that was found on-device (or a WARN if none). Displayed in the Diagnostics
 * panel in Settings → About so field issues can be diagnosed without logcat.
 */
object TwaLaunchLog {

    private const val FILE_NAME = "twa_launch.log"
    private const val MAX_ENTRIES = 100

    @Synchronized
    fun append(context: Context, message: String) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val file = logFile(context)
        val lines = if (file.exists()) file.readLines().toMutableList() else mutableListOf()
        while (lines.size >= MAX_ENTRIES) lines.removeAt(0)
        lines.add("[$ts] $message")
        file.writeText(lines.joinToString("\n") + "\n")
    }

    fun read(context: Context): String {
        val file = logFile(context)
        return if (file.exists() && file.length() > 0) file.readText().trimEnd()
        else "(no entries yet)"
    }

    private fun logFile(context: Context) = File(context.filesDir, FILE_NAME)
}
