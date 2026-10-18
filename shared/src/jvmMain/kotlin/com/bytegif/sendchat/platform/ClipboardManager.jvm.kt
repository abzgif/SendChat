package com.bytegif.sendchat.platform

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

actual class ClipboardManager actual constructor() {
    private var executor: ScheduledExecutorService? = null
    private var lastObservedText: String? = null

    actual fun getText(): String? {
        return runCatching {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                clipboard.getData(DataFlavor.stringFlavor) as? String
            } else null
        }.getOrNull()
    }

    actual fun setText(text: String) {
        runCatching {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val selection = StringSelection(text)
            clipboard.setContents(selection, selection)
            lastObservedText = text
        }
    }

    actual fun startObserving(onClipboardChanged: (String) -> Unit) {
        stopObserving()
        lastObservedText = getText()
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        executor = scheduler
        scheduler.scheduleWithFixedDelay({
            try {
                val current = getText()
                if (!current.isNullOrBlank() && current != lastObservedText) {
                    lastObservedText = current
                    onClipboardChanged(current)
                }
            } catch (_: Exception) {
            }
        }, 1, 1, TimeUnit.SECONDS)
    }

    actual fun stopObserving() {
        executor?.shutdownNow()
        executor = null
    }
}
