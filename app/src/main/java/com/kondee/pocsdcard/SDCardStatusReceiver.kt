package com.kondee.pocsdcard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SDCardStatusReceiver(
    private val onMountStatusChangedCallback: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action) {
            Intent.ACTION_MEDIA_MOUNTED, Intent.ACTION_MEDIA_UNMOUNTED -> {
                onMountStatusChangedCallback()
            }
        }
    }
}