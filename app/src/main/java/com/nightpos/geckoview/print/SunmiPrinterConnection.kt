package com.nightpos.geckoview.print

import android.content.Context
import android.graphics.Bitmap
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService

/**
 * Thin wrapper around Sunmi's [SunmiPrinterService] AIDL binding — the only way
 * to reach the T1's *internal* thermal printer (there's no generic Android print
 * HAL for it). [SunmiPrintService] delegates every queued job here.
 *
 * Connecting is asynchronous (`bindService` → [InnerPrinterCallback.onConnected]),
 * so [awaitReady] briefly polls for the service reference to appear before a job
 * is rendered/sent. This is safe to block on: print jobs already run on a
 * background thread spawned from [android.printservice.PrintService.onPrintJobQueued].
 */
class SunmiPrinterConnection(private val context: Context) {

    @Volatile
    private var service: SunmiPrinterService? = null

    private val callback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService) {
            this@SunmiPrinterConnection.service = service
        }

        override fun onDisconnected() {
            service = null
        }
    }

    fun bind(): Boolean =
        runCatching { InnerPrinterManager.getInstance().bindService(context, callback) }
            .getOrDefault(false)

    fun unbind() {
        runCatching { InnerPrinterManager.getInstance().unBindService(context, callback) }
        service = null
    }

    fun awaitReady(timeoutMs: Long = CONNECT_TIMEOUT_MS): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (service == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return service != null
    }

    /**
     * Prints [bitmap] (already scaled to the printer's fixed dot-width), feeds
     * the paper clear of the cutter, then cuts. Mirrors Sunmi's own demo, which
     * passes a null result-callback for these calls — each AIDL call blocks
     * briefly on the binder thread until the printer accepts the command, which
     * is enough ordering guarantee for sequential single-page receipts.
     */
    fun printReceipt(bitmap: Bitmap): Boolean {
        val svc = service ?: return false
        return runCatching {
            svc.printBitmap(bitmap, null)
            svc.lineWrap(FEED_LINES_AFTER_PRINT, null)
            svc.cutPaper(null)
        }.isSuccess
    }

    /** Sends raw ESC/POS bytes directly — used by the JS bridge for Odoo POS printing. */
    fun sendRaw(bytes: ByteArray): Boolean {
        val svc = service ?: return false
        return runCatching { svc.sendRAWData(bytes, null) }.isSuccess
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000L
        private const val POLL_INTERVAL_MS = 50L
        private const val FEED_LINES_AFTER_PRINT = 4
    }
}
