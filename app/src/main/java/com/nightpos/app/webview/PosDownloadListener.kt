package com.nightpos.app.webview

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast

/**
 * Hands off WebView downloads (e.g. PDF receipts/reports exported from Odoo) to
 * the system [DownloadManager] so they show up in the notification shade and the
 * device's Downloads app — the standard, reliable way to support downloads from
 * a WebView without bundling a custom download UI.
 *
 * The current session's cookies are forwarded so authenticated download URLs work.
 */
class PosDownloadListener(private val context: Context) : DownloadListener {

    override fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
    ) {
        runCatching {
            val uri = Uri.parse(url)
            val cookies = CookieManager.getInstance().getCookie(url)
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)

            val request = DownloadManager.Request(uri).apply {
                setMimeType(resolveMimeType(mimeType, fileName))
                addRequestHeader("User-Agent", userAgent)
                if (!cookies.isNullOrBlank()) addRequestHeader("Cookie", cookies)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(context, "กำลังดาวน์โหลด: $fileName", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "ไม่สามารถเริ่มการดาวน์โหลดได้", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveMimeType(mimeType: String?, fileName: String): String {
        if (!mimeType.isNullOrBlank() && mimeType != "application/octet-stream") return mimeType
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }
}
