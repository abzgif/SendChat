package com.bytegif.sendchat.network.server

import com.bytegif.sendchat.data.file.FileManager
import com.bytegif.sendchat.data.file.isImageFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

@Serializable
data class WebSharedFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: Long,
    val filePath: String,
    val isImage: Boolean
)

@Serializable
data class WebMessageRequest(
    val text: String
)

@Serializable
data class WebChatMessage(
    val id: String,
    val text: String,
    val fromApp: Boolean,
    val timestamp: Long,
    val isFile: Boolean = false,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val fileDownloadId: String? = null
)

class WebShareManager {
    private val _sharedFiles = MutableStateFlow<List<WebSharedFile>>(emptyList())
    val sharedFiles: StateFlow<List<WebSharedFile>> = _sharedFiles.asStateFlow()

    fun addFile(filePath: String): WebSharedFile? {
        if (!FileManager.fileExists(filePath)) return null
        val name = FileManager.getFileName(filePath)
        val size = FileManager.getFileSize(filePath)
        val item = WebSharedFile(
            name = name,
            size = size,
            filePath = filePath,
            isImage = isImageFile(name)
        )
        _sharedFiles.value = _sharedFiles.value.filter { it.filePath != filePath } + item
        return item
    }

    fun removeFile(id: String) {
        _sharedFiles.value = _sharedFiles.value.filter { it.id != id }
    }

    fun clear() {
        _sharedFiles.value = emptyList()
    }

    fun getFile(id: String): WebSharedFile? {
        return _sharedFiles.value.firstOrNull { it.id == id }
    }

    fun getHtml(deviceName: String): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>SendChat - Local Web Share</title>
    <style>
        :root {
            --bg-primary: #0b0f19;
            --bg-card: #151d2f;
            --bg-card-hover: #1e2942;
            --text-primary: #f8fafc;
            --text-secondary: #94a3b8;
            --accent: #FF5365;
            --accent-hover: #e04455;
            --primary: #3b82f6;
            --border: rgba(255, 255, 255, 0.08);
            --border-active: rgba(255, 83, 101, 0.5);
            --success: #10b981;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body {
            background-color: var(--bg-primary);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 16px;
        }
        .container {
            width: 100%;
            max-width: 540px;
            display: flex;
            flex-direction: column;
            gap: 16px;
            margin-top: 8px;
        }
        .header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 16px 20px;
            background: var(--bg-card);
            border-radius: 18px;
            border: 1px solid var(--border);
        }
        .logo-area {
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .logo-icon {
            width: 38px;
            height: 38px;
            background: linear-gradient(135deg, #FF5365, #FF7B89);
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            box-shadow: 0 4px 12px rgba(255, 83, 101, 0.3);
        }
        .app-name {
            font-size: 18px;
            font-weight: 700;
            letter-spacing: -0.3px;
        }
        .device-badge {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 13px;
            color: var(--text-secondary);
            margin-top: 2px;
        }
        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: var(--success);
            box-shadow: 0 0 8px rgba(16, 185, 129, 0.6);
        }
        .tabs {
            display: flex;
            background: var(--bg-card);
            border-radius: 14px;
            padding: 4px;
            border: 1px solid var(--border);
            gap: 4px;
        }
        .tab-btn {
            flex: 1;
            padding: 10px 14px;
            border: none;
            background: transparent;
            color: var(--text-secondary);
            font-size: 14px;
            font-weight: 600;
            border-radius: 10px;
            cursor: pointer;
            transition: all 0.2s ease;
        }
        .tab-btn.active {
            background: var(--accent);
            color: #ffffff;
            box-shadow: 0 2px 8px rgba(255, 83, 101, 0.3);
        }
        .tab-content {
            background: var(--bg-card);
            border-radius: 18px;
            border: 1px solid var(--border);
            padding: 20px;
            display: none;
            flex-direction: column;
            gap: 16px;
        }
        .tab-content.active {
            display: flex;
        }
        .dropzone {
            border: 2px dashed rgba(255, 255, 255, 0.15);
            border-radius: 14px;
            padding: 32px 16px;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s ease;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 10px;
        }
        .dropzone:hover, .dropzone.dragover {
            border-color: var(--accent);
            background: rgba(255, 83, 101, 0.05);
        }
        .dropzone-icon {
            font-size: 40px;
        }
        .dropzone-text {
            font-size: 15px;
            font-weight: 600;
            color: var(--text-primary);
        }
        .dropzone-sub {
            font-size: 13px;
            color: var(--text-secondary);
        }
        .btn {
            background: var(--accent);
            color: #ffffff;
            border: none;
            border-radius: 12px;
            padding: 12px 20px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s ease;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
        }
        .btn:hover {
            background: var(--accent-hover);
        }
        .btn-secondary {
            background: rgba(255, 255, 255, 0.08);
            color: var(--text-primary);
        }
        .btn-secondary:hover {
            background: rgba(255, 255, 255, 0.12);
        }
        .file-list {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        .file-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: rgba(255, 255, 255, 0.03);
            border: 1px solid var(--border);
            padding: 12px 16px;
            border-radius: 12px;
        }
        .file-info {
            display: flex;
            flex-direction: column;
            gap: 2px;
            overflow: hidden;
            padding-right: 12px;
        }
        .file-name {
            font-size: 14px;
            font-weight: 600;
            color: var(--text-primary);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .file-size {
            font-size: 12px;
            color: var(--text-secondary);
        }
        .progress-box {
            display: none;
            flex-direction: column;
            gap: 8px;
            margin-top: 10px;
        }
        .progress-bar-bg {
            width: 100%;
            height: 8px;
            background: rgba(255, 255, 255, 0.1);
            border-radius: 4px;
            overflow: hidden;
        }
        .progress-bar-fill {
            height: 100%;
            background: var(--accent);
            width: 0%;
            transition: width 0.1s linear;
        }
        .progress-label {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: var(--text-secondary);
        }
        textarea {
            width: 100%;
            background: rgba(255, 255, 255, 0.04);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 12px;
            color: var(--text-primary);
            font-size: 14px;
            resize: vertical;
            min-height: 70px;
            outline: none;
            box-sizing: border-box;
            font-family: inherit;
        }
        textarea:focus {
            border-color: var(--accent);
        }
        .chat-container {
            background: rgba(0, 0, 0, 0.25);
            border: 1px solid var(--border);
            border-radius: 14px;
            padding: 14px;
            max-height: 400px;
            min-height: 200px;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            gap: 12px;
            box-sizing: border-box;
        }
        .chat-history {
            display: flex;
            flex-direction: column;
            gap: 12px;
            width: 100%;
        }
        .chat-msg {
            display: flex;
            flex-direction: column;
            max-width: 86%;
            word-break: break-word;
        }
        .chat-msg.incoming {
            align-self: flex-start;
        }
        .chat-msg.outgoing {
            align-self: flex-end;
        }
        .chat-meta {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 11px;
            color: var(--text-secondary);
            margin-bottom: 4px;
            padding: 0 4px;
        }
        .chat-msg.outgoing .chat-meta {
            justify-content: flex-end;
        }
        .chat-bubble {
            padding: 10px 14px;
            border-radius: 14px;
            font-size: 14px;
            line-height: 1.45;
            position: relative;
        }
        .chat-msg.incoming .chat-bubble {
            background: var(--bg-card-hover);
            color: var(--text-primary);
            border: 1px solid var(--border);
            border-top-left-radius: 4px;
        }
        .chat-msg.outgoing .chat-bubble {
            background: #2a3854;
            color: #ffffff;
            border: 1px solid rgba(255, 255, 255, 0.12);
            border-top-right-radius: 4px;
        }
        .chat-bubble-text {
            white-space: pre-wrap;
            user-select: text;
        }
        .chat-footer {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 8px;
            margin-top: 6px;
            font-size: 11px;
            opacity: 0.9;
        }
        .chat-time {
            font-size: 11px;
            color: var(--text-secondary);
        }
        .chat-actions {
            display: inline-flex;
            gap: 6px;
        }
        .copy-pill {
            background: rgba(255, 255, 255, 0.08);
            border: 1px solid rgba(255, 255, 255, 0.14);
            color: var(--text-primary);
            border-radius: 6px;
            padding: 2px 8px;
            font-size: 11px;
            cursor: pointer;
            transition: all 0.2s ease;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }
        .copy-pill:hover {
            background: rgba(255, 255, 255, 0.16);
            border-color: var(--accent);
        }
        .copy-pill.copied {
            background: rgba(16, 185, 129, 0.25);
            border-color: var(--success);
            color: var(--success);
        }
        .chat-file-box {
            display: flex;
            align-items: center;
            gap: 10px;
            background: rgba(0, 0, 0, 0.25);
            padding: 8px 12px;
            border-radius: 10px;
            margin-top: 4px;
        }
        .empty-state {
            text-align: center;
            padding: 32px 16px;
            color: var(--text-secondary);
            font-size: 14px;
            line-height: 1.5;
        }
        .toast {
            position: fixed;
            bottom: 24px;
            left: 50%;
            transform: translateX(-50%) translateY(100px);
            background: #1e293b;
            color: #ffffff;
            padding: 12px 24px;
            border-radius: 24px;
            font-size: 14px;
            font-weight: 500;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
            border: 1px solid rgba(255, 255, 255, 0.1);
            transition: transform 0.3s cubic-bezier(0.18, 0.89, 0.32, 1.28);
            z-index: 999;
            pointer-events: none;
        }
        .toast.show {
            transform: translateX(-50%) translateY(0);
        }
    </style>
</head>
<body>

<div class="container">
    <div class="header">
        <div class="logo-area">
            <div class="logo-icon">⚡</div>
            <div>
                <div class="app-name">SendChat Web</div>
                <div class="device-badge">
                    <span class="status-dot"></span>
                    <span>Connected to <strong>$deviceName</strong></span>
                </div>
            </div>
        </div>
    </div>

    <div class="tabs">
        <button class="tab-btn active" onclick="switchTab('upload')">Send to Device</button>
        <button class="tab-btn" onclick="switchTab('download')">Download Files</button>
        <button class="tab-btn" onclick="switchTab('message')">Send Text</button>
    </div>

    <!-- TAB 1: UPLOAD FILES -->
    <div id="tab-upload" class="tab-content active">
        <div class="dropzone" id="dropzone" onclick="document.getElementById('fileInput').click()">
            <div class="dropzone-icon">📁</div>
            <div class="dropzone-text">Tap or drag files here</div>
            <div class="dropzone-sub">Share photos, videos, APKs, or documents</div>
            <input type="file" id="fileInput" multiple style="display: none;" onchange="handleFileSelect(this.files)">
        </div>

        <div id="selectedFilesBox" style="display:none; flex-direction: column; gap: 8px;">
            <div style="font-size: 13px; font-weight: 600; color: var(--text-secondary);" id="selectedCountText">Selected Files</div>
            <div class="file-list" id="selectedFileList"></div>
            <button class="btn" id="uploadBtn" onclick="startUpload()" style="margin-top: 8px;">
                <span>Send to $deviceName</span>
            </button>
        </div>

        <div class="progress-box" id="progressBox">
            <div class="progress-bar-bg">
                <div class="progress-bar-fill" id="progressBar"></div>
            </div>
            <div class="progress-label">
                <span id="progressStatus">Uploading...</span>
                <span id="progressPercent">0%</span>
            </div>
        </div>
    </div>

    <!-- TAB 2: DOWNLOAD SHARED FILES -->
    <div id="tab-download" class="tab-content">
        <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-size: 14px; font-weight: 600; color: var(--text-secondary);">Files Shared by $deviceName</span>
            <button class="btn btn-secondary" onclick="loadSharedFiles()" style="padding: 6px 12px; font-size: 12px;">Refresh</button>
        </div>
        <div class="file-list" id="sharedFilesList">
            <div class="empty-state">Loading files...</div>
        </div>
    </div>

    <!-- TAB 3: SEND TEXT & CHAT HISTORY -->
    <div id="tab-message" class="tab-content">
        <div style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-size: 14px; font-weight: 600; color: var(--text-secondary);">Chat with $deviceName</span>
            <button class="btn btn-secondary" onclick="loadMessages()" style="padding: 6px 12px; font-size: 12px;">Refresh</button>
        </div>

        <div class="chat-container" id="chatContainer">
            <div class="chat-history" id="chatHistory">
                <div class="empty-state">Loading chat history...</div>
            </div>
        </div>

        <div style="display: flex; flex-direction: column; gap: 8px; margin-top: 4px;">
            <textarea id="messageText" placeholder="Type a message or paste a link... (Press Enter to send)" rows="2"></textarea>
            <button class="btn" id="sendBtn" onclick="sendMessage()">Send Message</button>
        </div>
    </div>
</div>

<div class="toast" id="toast"></div>

<script>
    let filesToUpload = [];

    function switchTab(tab) {
        document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

        if (tab === 'upload') {
            document.querySelectorAll('.tab-btn')[0].classList.add('active');
            document.getElementById('tab-upload').classList.add('active');
        } else if (tab === 'download') {
            document.querySelectorAll('.tab-btn')[1].classList.add('active');
            document.getElementById('tab-download').classList.add('active');
            loadSharedFiles();
        } else if (tab === 'message') {
            document.querySelectorAll('.tab-btn')[2].classList.add('active');
            document.getElementById('tab-message').classList.add('active');
            loadMessages();
        }
    }

    function showToast(msg) {
        const toast = document.getElementById('toast');
        toast.textContent = msg;
        toast.classList.add('show');
        setTimeout(() => toast.classList.remove('show'), 2500);
    }

    function formatBytes(bytes) {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    }

    function formatTime(timestamp) {
        if (!timestamp) return '';
        const d = new Date(timestamp);
        const now = new Date();
        const isToday = d.toDateString() === now.toDateString();
        const timeStr = d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit', hour12: true });
        if (isToday) return timeStr;
        const dateStr = d.toLocaleDateString([], { month: 'short', day: 'numeric' });
        return `${'$'}{dateStr}, ${'$'}{timeStr}`;
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
    }

    function copyText(text, btn) {
        if (!text) return;
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(() => {
                onCopied(btn);
            }).catch(() => fallbackCopy(text, btn));
        } else {
            fallbackCopy(text, btn);
        }
    }

    function fallbackCopy(text, btn) {
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        try {
            document.execCommand('copy');
            onCopied(btn);
        } catch (e) {
            showToast('Failed to copy text');
        }
        document.body.removeChild(ta);
    }

    function onCopied(btn) {
        if (btn) {
            btn.classList.add('copied');
            const originalHtml = btn.innerHTML;
            btn.innerHTML = '<span>✓ Copied</span>';
            setTimeout(() => {
                btn.classList.remove('copied');
                btn.innerHTML = originalHtml;
            }, 2000);
        }
        showToast('Text copied to clipboard!');
    }

    // Drag and drop setup
    const dropzone = document.getElementById('dropzone');
    ['dragenter', 'dragover'].forEach(e => dropzone.addEventListener(e, (ev) => { ev.preventDefault(); dropzone.classList.add('dragover'); }));
    ['dragleave', 'drop'].forEach(e => dropzone.addEventListener(e, (ev) => { ev.preventDefault(); dropzone.classList.remove('dragover'); }));
    dropzone.addEventListener('drop', (ev) => {
        if (ev.dataTransfer.files) handleFileSelect(ev.dataTransfer.files);
    });

    function handleFileSelect(files) {
        if (!files || files.length === 0) return;
        filesToUpload = Array.from(files);
        renderSelectedFiles();
    }

    function renderSelectedFiles() {
        const box = document.getElementById('selectedFilesBox');
        const list = document.getElementById('selectedFileList');
        list.innerHTML = '';
        if (filesToUpload.length === 0) {
            box.style.display = 'none';
            return;
        }
        box.style.display = 'flex';
        document.getElementById('selectedCountText').textContent = filesToUpload.length + ' File(s) Ready to Send';

        filesToUpload.forEach(f => {
            const item = document.createElement('div');
            item.className = 'file-item';
            item.innerHTML = `
                <div class="file-info">
                    <div class="file-name">${'$'}{f.name}</div>
                    <div class="file-size">${'$'}{formatBytes(f.size)}</div>
                </div>
            `;
            list.appendChild(item);
        });
    }

    async function startUpload() {
        if (filesToUpload.length === 0) return;
        const btn = document.getElementById('uploadBtn');
        const progressBox = document.getElementById('progressBox');
        const progressBar = document.getElementById('progressBar');
        const progressStatus = document.getElementById('progressStatus');
        const progressPercent = document.getElementById('progressPercent');

        btn.disabled = true;
        progressBox.style.display = 'flex';

        for (let i = 0; i < filesToUpload.length; i++) {
            const file = filesToUpload[i];
            progressStatus.textContent = `Uploading (${'$'}{i + 1}/${'$'}{filesToUpload.length}): ${'$'}{file.name}`;
            await uploadSingleFile(file, (pct) => {
                progressBar.style.width = pct + '%';
                progressPercent.textContent = pct + '%';
            });
        }

        btn.disabled = false;
        progressBox.style.display = 'none';
        filesToUpload = [];
        renderSelectedFiles();
        showToast('All files sent successfully!');
    }

    function uploadSingleFile(file, onProgress) {
        return new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.open('POST', '/api/web/upload');
            xhr.setRequestHeader('X-Filename', encodeURIComponent(file.name));
            xhr.setRequestHeader('X-Filesize', file.size.toString());

            xhr.upload.onprogress = (e) => {
                if (e.lengthComputable) {
                    const pct = Math.round((e.loaded / e.total) * 100);
                    onProgress(pct);
                }
            };

            xhr.onload = () => {
                if (xhr.status >= 200 && xhr.status < 300) resolve();
                else reject(new Error('Upload failed'));
            };
            xhr.onerror = () => reject(new Error('Network error'));
            xhr.send(file);
        });
    }

    async function loadSharedFiles() {
        const container = document.getElementById('sharedFilesList');
        try {
            const res = await fetch('/api/web/files');
            const files = await res.json();
            container.innerHTML = '';
            if (!files || files.length === 0) {
                container.innerHTML = '<div class="empty-state">No files are currently shared by $deviceName.<br>Shared files from the app will appear here.</div>';
                return;
            }
            files.forEach(f => {
                const item = document.createElement('div');
                item.className = 'file-item';
                item.innerHTML = `
                    <div class="file-info">
                        <div class="file-name">${'$'}{f.name}</div>
                        <div class="file-size">${'$'}{formatBytes(f.size)}</div>
                    </div>
                    <a href="/api/web/download/${'$'}{f.id}" class="btn" download="${'$'}{f.name}" style="padding: 8px 14px; font-size: 13px; text-decoration: none;">Download</a>
                `;
                container.appendChild(item);
            });
        } catch (e) {
            container.innerHTML = '<div class="empty-state">Could not load shared files.</div>';
        }
    }

    async function loadMessages() {
        const history = document.getElementById('chatHistory');
        try {
            const res = await fetch('/api/web/messages');
            if (!res.ok) return;
            const messages = await res.json();
            if (!messages || messages.length === 0) {
                history.innerHTML = '<div class="empty-state">No messages yet.<br>Send a message below or from $deviceName to chat.</div>';
                return;
            }

            const signature = messages.length + '_' + (messages[messages.length - 1].id || '');
            if (history.dataset.sig === signature) return;
            history.dataset.sig = signature;

            history.innerHTML = '';
            messages.forEach((msg, idx) => {
                const isIncoming = msg.fromApp;
                const senderName = isIncoming ? '$deviceName' : 'You';
                const timeText = formatTime(msg.timestamp);

                const msgDiv = document.createElement('div');
                msgDiv.className = 'chat-msg ' + (isIncoming ? 'incoming' : 'outgoing');

                let contentHtml = '';
                if (msg.isFile) {
                    const downloadUrl = msg.fileDownloadId ? ('/api/web/download/' + msg.fileDownloadId) : '#';
                    contentHtml = `
                        <div class="chat-file-box">
                            <span style="font-size: 22px;">📁</span>
                            <div style="flex: 1; overflow: hidden; padding-right: 8px;">
                                <div style="font-weight: 600; font-size: 13px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">${'$'}{escapeHtml(msg.fileName || 'Shared File')}</div>
                                <div style="font-size: 11px; color: var(--text-secondary);">${'$'}{formatBytes(msg.fileSize || 0)}</div>
                            </div>
                            <a href="${'$'}{downloadUrl}" download="${'$'}{escapeHtml(msg.fileName || 'download')}" class="btn" style="padding: 6px 12px; font-size: 12px; text-decoration: none;">Download</a>
                        </div>
                    `;
                } else {
                    contentHtml = `<div class="chat-bubble-text">${'$'}{escapeHtml(msg.text)}</div>`;
                }

                msgDiv.innerHTML = `
                    <div class="chat-meta">
                        <span>${'$'}{senderName}</span>
                    </div>
                    <div class="chat-bubble">
                        ${'$'}{contentHtml}
                        <div class="chat-footer">
                            <span class="chat-time">${'$'}{timeText}</span>
                            <div class="chat-actions">
                                <button class="copy-pill">
                                    <span>📋 Copy</span>
                                </button>
                            </div>
                        </div>
                    </div>
                `;
                history.appendChild(msgDiv);

                const copyBtn = msgDiv.querySelector('.copy-pill');
                if (copyBtn) {
                    copyBtn.addEventListener('click', () => copyText(msg.text, copyBtn));
                }
            });

            const container = document.getElementById('chatContainer');
            if (container) {
                container.scrollTop = container.scrollHeight;
            }
        } catch (e) {
            // ignore network polling error
        }
    }

    async function sendMessage() {
        const input = document.getElementById('messageText');
        const text = input.value.trim();
        if (!text) return;
        const btn = document.getElementById('sendBtn');
        if (btn) btn.disabled = true;
        try {
            const res = await fetch('/api/web/message', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ text: text })
            });
            if (res.ok) {
                input.value = '';
                showToast('Message sent to $deviceName!');
                loadMessages();
            } else {
                showToast('Failed to send message');
            }
        } catch (e) {
            showToast('Network error');
        } finally {
            if (btn) btn.disabled = false;
        }
    }

    const msgInput = document.getElementById('messageText');
    if (msgInput) {
        msgInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
    }

    // Load initial messages and files
    loadMessages();
    loadSharedFiles();

    // Auto-refresh chat messages and shared files every 2.5 seconds
    setInterval(loadMessages, 2500);
    setInterval(loadSharedFiles, 3000);
</script>

</body>
</html>
        """.trimIndent()
    }
}
