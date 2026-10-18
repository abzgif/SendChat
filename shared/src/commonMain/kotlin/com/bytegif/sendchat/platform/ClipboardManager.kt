package com.bytegif.sendchat.platform

expect class ClipboardManager() {
    fun getText(): String?
    fun setText(text: String)
    fun startObserving(onClipboardChanged: (String) -> Unit)
    fun stopObserving()
}
