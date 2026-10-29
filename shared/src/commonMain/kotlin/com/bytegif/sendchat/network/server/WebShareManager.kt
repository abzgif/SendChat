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

class WebShareManager {
    private val _sharedFiles = MutableStateFlow<List<WebSharedFile>>(emptyList())
    val sharedFiles: StateFlow<List<WebSharedFile>> = _sharedFiles.asStateFlow()

    fun addFile(filePath: String): WebSharedFile? {
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) return null
        val item = WebSharedFile(
            name = file.name,
            size = file.length(),
            filePath = file.absolutePath,
            isImage = isImageFile(file.name)
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
            min-height: 90px;
            outline: none;
        }
        textarea:focus {
            border-color: var(--accent);
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

    <!-- TAB 3: SEND TEXT / LINK -->
    <div id="tab-message" class="tab-content">
        <div style="font-size: 14px; font-weight: 600; color: var(--text-secondary);">Send Text or Link to $deviceName</div>
        <textarea id="messageText" placeholder="Paste a link or type a message..."></textarea>
        <button class="btn" onclick="sendMessage()">Send Message</button>
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

    async function sendMessage() {
        const input = document.getElementById('messageText');
        const text = input.value.trim();
        if (!text) return;
        try {
            const res = await fetch('/api/web/message', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ text: text })
            });
            if (res.ok) {
                input.value = '';
                showToast('Message sent to $deviceName!');
            } else {
                showToast('Failed to send message');
            }
        } catch (e) {
            showToast('Network error');
        }
    }
</script>

</body>
</html>
        """.trimIndent()
    }
}
