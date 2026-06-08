package com.nightpos.app.print

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.printservice.PrintJob
import android.printservice.PrintService
import android.printservice.PrinterDiscoverySession

/**
 * Exposes the Sunmi T1's internal thermal printer through Android's system
 * print framework (Settings → Printing) so receipts can be printed from *any*
 * app — including the external Firefox browser that Odoo destinations now open
 * in (see [com.nightpos.app.twa.TwaLauncherActivity]) — via the standard system
 * "Print" action / share-sheet entry.
 *
 * No JS bridge or Odoo-side template changes are needed: Odoo's existing
 * `window.print()` receipt flow works as soon as staff pick "Sunmi Internal
 * Printer" from the system print dialog (and it can be set as the default).
 */
class SunmiPrintService : PrintService() {

    private var connection: SunmiPrinterConnection? = null

    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession =
        SunmiPrinterDiscoverySession(this)

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        printJob.cancel()
    }

    override fun onPrintJobQueued(printJob: PrintJob) {
        printJob.start()
        Thread { renderAndPrint(printJob) }.start()
    }

    override fun onDestroy() {
        connection?.unbind()
        connection = null
        super.onDestroy()
    }

    private fun renderAndPrint(printJob: PrintJob) {
        val pfd = printJob.document?.data
        if (pfd == null) {
            printJob.fail("No document data")
            return
        }

        val printerConnection = connection ?: SunmiPrinterConnection(applicationContext).also {
            connection = it
            it.bind()
        }
        if (!printerConnection.awaitReady()) {
            printJob.fail("Sunmi printer service unavailable")
            return
        }

        val rendered = runCatching {
            PdfRenderer(pfd).use { renderer ->
                for (pageIndex in 0 until renderer.pageCount) {
                    renderer.openPage(pageIndex).use { page ->
                        val bitmap = renderPageForThermalPrint(page)
                        printerConnection.printReceipt(bitmap)
                        bitmap.recycle()
                    }
                }
            }
        }

        if (rendered.isSuccess) printJob.complete() else printJob.fail("Failed to render or print the document")
    }

    /**
     * Scales the rendered page to the printer's fixed dot-width and flattens it
     * onto an opaque white background — thermal heads only print black/white,
     * so any transparency in the source PDF would otherwise come out solid black.
     */
    private fun renderPageForThermalPrint(page: PdfRenderer.Page): Bitmap {
        val scale = PRINTER_WIDTH_PX.toFloat() / page.width
        val height = (page.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(PRINTER_WIDTH_PX, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, Matrix().apply { setScale(scale, scale) }, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
        return bitmap
    }

    companion object {
        // 58mm thermal paper renders at 384 dots wide on the T1's print head;
        // change to 576 if targeting an 80mm-paper Sunmi variant instead.
        private const val PRINTER_WIDTH_PX = 384
    }
}
