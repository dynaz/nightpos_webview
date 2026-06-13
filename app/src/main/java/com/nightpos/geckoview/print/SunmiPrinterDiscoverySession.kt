package com.nightpos.geckoview.print

import android.print.PrintAttributes
import android.print.PrinterCapabilitiesInfo
import android.print.PrinterId
import android.print.PrinterInfo
import android.printservice.PrintService
import android.printservice.PrinterDiscoverySession

/**
 * Advertises a single virtual printer — the Sunmi T1's internal thermal unit —
 * to Android's system print framework. There's nothing to actually "discover"
 * (it's always present and is always the same device), so this just publishes
 * one [PrinterInfo] whose capabilities match 58mm thermal receipt paper.
 */
class SunmiPrinterDiscoverySession(private val service: PrintService) : PrinterDiscoverySession() {

    override fun onStartPrinterDiscovery(priorityList: MutableList<PrinterId>) {
        addPrinters(listOf(buildPrinterInfo(service.generatePrinterId(PRINTER_LOCAL_ID))))
    }

    override fun onStopPrinterDiscovery() = Unit

    override fun onValidatePrinters(printerIds: MutableList<PrinterId>) = Unit

    override fun onStartPrinterStateTracking(printerId: PrinterId) {
        addPrinters(listOf(buildPrinterInfo(printerId)))
    }

    override fun onStopPrinterStateTracking(printerId: PrinterId) = Unit

    override fun onDestroy() = Unit

    private fun buildPrinterInfo(id: PrinterId): PrinterInfo =
        PrinterInfo.Builder(id, PRINTER_LABEL, PrinterInfo.STATUS_IDLE)
            .setCapabilities(buildCapabilities(id))
            .build()

    /**
     * 58mm thermal paper is ~2283 mils wide; the T1's head resolution is
     * 203dpi. Receipt length is open-ended, so a generous fixed page length is
     * advertised — Odoo's print stylesheet paginates its own receipt template
     * regardless of what the nominal "page" measures.
     */
    private fun buildCapabilities(id: PrinterId): PrinterCapabilitiesInfo {
        val receiptMediaSize = PrintAttributes.MediaSize(
            "sunmi_receipt_58mm",
            "58mm receipt",
            RECEIPT_WIDTH_MILS,
            RECEIPT_HEIGHT_MILS,
        )
        val resolution = PrintAttributes.Resolution(
            "sunmi_203dpi",
            "203 dpi",
            PRINTER_DPI,
            PRINTER_DPI,
        )
        return PrinterCapabilitiesInfo.Builder(id)
            .addMediaSize(receiptMediaSize, true)
            .addResolution(resolution, true)
            .setColorModes(PrintAttributes.COLOR_MODE_MONOCHROME, PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
    }

    companion object {
        private const val PRINTER_LOCAL_ID = "sunmi_internal_printer"
        private const val PRINTER_LABEL = "Sunmi Internal Printer"
        private const val RECEIPT_WIDTH_MILS = 2_283
        private const val RECEIPT_HEIGHT_MILS = 11_000
        private const val PRINTER_DPI = 203
    }
}
