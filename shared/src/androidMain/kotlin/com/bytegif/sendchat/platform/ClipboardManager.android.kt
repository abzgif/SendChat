package com.bytegif.sendchat.platform

import android.content.ClipData
import android.content.ClipboardManager as AndroidClipboard
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bytegif.sendchat.data.db.AndroidAppContextHolder

actual class ClipboardManager actual constructor() {

    private var clipListener: AndroidClipboard.OnPrimaryClipChangedListener? = null
    private var lastObservedText: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun getSystemService(): AndroidClipboard? {
        val ctx = AndroidAppContextHolder.appContext ?: return null
        return ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? AndroidClipboard
    }

    actual fun getText(): String? {
        return runCatching {
            val manager = getSystemService() ?: return null
            val clip = manager.primaryClip ?: return null
            if (clip.itemCount > 0) {
                clip.getItemAt(0).text?.toString()
            } else null
        }.getOrNull()
    }

    actual fun setText(text: String) {
        lastObservedText = text
        mainHandler.post {
            runCatching {
                val manager = getSystemService() ?: return@runCatching
                val clip = ClipData.newPlainText("SendChat", text)
                manager.setPrimaryClip(clip)
            }
        }
    }

    actual fun startObserving(onClipboardChanged: (String) -> Unit) {
        stopObserving()
        mainHandler.post {
            val manager = getSystemService() ?: return@post
            lastObservedText = getText()

            val listener = AndroidClipboard.OnPrimaryClipChangedListener {
                val current = getText()
                if (!current.isNullOrBlank() && current != lastObservedText) {
                    lastObservedText = current
                    onClipboardChanged(current)
                }
            }
            clipListener = listener
            runCatching {
                manager.addPrimaryClipChangedListener(listener)
            }
        }
    }

    actual fun stopObserving() {
        mainHandler.post {
            val manager = getSystemService() ?: return@post
            clipListener?.let {
                runCatching { manager.removePrimaryClipChangedListener(it) }
                clipListener = null
            }
        }
    }
}
