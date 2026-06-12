package com.nightpos.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nightpos.app.MainActivity
import com.nightpos.app.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = PreferencesManager(context.applicationContext)
                    .autoStartupEnabled
                    .first()
                if (enabled) {
                    context.startActivity(
                        Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
