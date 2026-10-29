package com.bytegif.sendchat.viewmodel

import com.bytegif.sendchat.domain.model.DevicePlatform
import com.bytegif.sendchat.platform.PlatformInfo
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

const val DEFAULT_APP_VERSION = "1.1.6"

expect object AppVersion {
    val current: String
}

data class UpdateRelease(
    val version: String,
    val title: String,
    val notes: String,
    val asset: UpdateAsset
)

data class UpdateAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

sealed interface UpdateState {
    data object PlayStoreManaged : UpdateState
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val latestVersion: String) : UpdateState
    data class Available(val release: UpdateRelease) : UpdateState
    data class Downloading(val release: UpdateRelease, val progress: Float?) : UpdateState
    data class ReadyToInstall(val release: UpdateRelease, val filePath: String) : UpdateState
    data class Failed(val message: String) : UpdateState
}

class UpdateViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow<UpdateState>(
        if (PlatformInfo.platform == DevicePlatform.ANDROID) UpdateState.PlayStoreManaged else UpdateState.Idle
    )
    val state: StateFlow<UpdateState> = mutableState.asStateFlow()

    fun checkForUpdates() {
        if (PlatformInfo.platform == DevicePlatform.ANDROID) {
            mutableState.value = UpdateState.PlayStoreManaged
            return
        }
        if (mutableState.value is UpdateState.Checking || mutableState.value is UpdateState.Downloading) return

        scope.launch {
            mutableState.value = UpdateState.Checking
            try {
                val release = withContext(Dispatchers.IO) { fetchLatestRelease() }
                if (compareVersions(release.tagName, AppVersion.current) <= 0) {
                    mutableState.value = UpdateState.UpToDate(release.tagName)
                } else {
                    val installer = selectInstaller(release.assets)
                        ?: error("Version ${release.tagName} is available, but its GitHub release has no installer for ${PlatformInfo.platform}.")

                    mutableState.value = UpdateState.Available(
                        UpdateRelease(
                            version = release.tagName,
                            title = release.name?.takeIf(String::isNotBlank) ?: "SendChat ${release.tagName}",
                            notes = release.body.orEmpty().trim(),
                            asset = UpdateAsset(installer.name, installer.downloadUrl, installer.size)
                        )
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = UpdateState.Failed(error.message ?: "Could not check GitHub for updates.")
            }
        }
    }

    fun downloadUpdate() {
        val available = mutableState.value as? UpdateState.Available ?: return
        scope.launch {
            mutableState.value = UpdateState.Downloading(available.release, null)
            try {
                val path = withContext(Dispatchers.IO) {
                    downloadInstaller(available.release) { progress ->
                        mutableState.value = UpdateState.Downloading(available.release, progress)
                    }
                }
                mutableState.value = UpdateState.ReadyToInstall(available.release, path)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = UpdateState.Failed(error.message ?: "Could not download the update.")
            }
        }
    }

    fun installUpdate() {
        val ready = mutableState.value as? UpdateState.ReadyToInstall ?: return
        val file = File(ready.filePath)
        if (!file.isFile) {
            mutableState.value = UpdateState.Failed("The downloaded installer could not be found. Check for updates and download it again.")
            return
        }

        runCatching { PlatformInfo.openFileInDefaultApp(file.absolutePath) }
            .onFailure { error ->
                mutableState.value = UpdateState.Failed(error.message ?: "Could not open the installer.")
            }
    }

    private fun fetchLatestRelease(): GithubRelease {
        val connection = (URL(RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "SendChat-Update-Checker")
        }

        try {
            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                error("No published stable release was found on GitHub yet.")
            }
            if (status == HttpURLConnection.HTTP_FORBIDDEN) {
                error("GitHub temporarily limited update checks. Please try again later.")
            }
            if (status !in 200..299) {
                error("GitHub update check failed (HTTP $status). Please try again later.")
            }

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val release = releaseJson.decodeFromString<GithubRelease>(json)
            val version = release.tagName.removePrefix("v").removePrefix("V")
            if (versionParts(version).isEmpty()) error("The latest GitHub release has an invalid version tag.")
            return release.copy(tagName = version)
        } finally {
            connection.disconnect()
        }
    }

    private fun selectInstaller(assets: List<GithubReleaseAsset>): GithubReleaseAsset? {
        val extensions = when (PlatformInfo.platform) {
            DevicePlatform.ANDROID -> emptyList()
            DevicePlatform.MACOS -> listOf("pkg", "dmg")
            DevicePlatform.WINDOWS -> listOf("msi", "exe")
            DevicePlatform.LINUX -> listOf("deb", "rpm")
            DevicePlatform.UNKNOWN -> emptyList()
        }

        return extensions.firstNotNullOfOrNull { extension ->
            assets.firstOrNull { it.name.substringAfterLast('.', "").equals(extension, ignoreCase = true) }
        }
    }

    private fun downloadInstaller(release: UpdateRelease, onProgress: (Float?) -> Unit): String {
        val updatesDirectory = File(PlatformInfo.getDefaultDownloadDirectory(), "Updates")
        if (!updatesDirectory.exists() && !updatesDirectory.mkdirs()) {
            error("Could not create the SendChat Updates folder.")
        }

        val safeAssetName = File(release.asset.name).name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val destination = File(updatesDirectory, "SendChat-${release.version}-$safeAssetName")
        val temporary = File(updatesDirectory, "${destination.name}.download")
        temporary.delete()

        val connection = (URL(release.asset.downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "SendChat-Update-Checker")
            setRequestProperty("Accept", "application/octet-stream")
        }

        try {
            val status = connection.responseCode
            if (status !in 200..299) error("GitHub download failed (HTTP $status). Please try again.")

            val totalBytes = connection.contentLengthLong.takeIf { it > 0L } ?: release.asset.sizeBytes
            var downloadedBytes = 0L
            var lastReportedPercent = -1
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloadedBytes += count

                        val progress = if (totalBytes > 0L) {
                            (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                        } else null
                        val percent = progress?.times(100f)?.toInt()
                        if (percent != null && percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            onProgress(progress)
                        }
                    }
                }
            }

            if (downloadedBytes == 0L) error("GitHub returned an empty installer.")
            if (release.asset.sizeBytes > 0L && downloadedBytes != release.asset.sizeBytes) {
                error("The installer download was incomplete. Please try again.")
            }
            if (destination.exists() && !destination.delete()) error("Could not replace the previous installer file.")
            if (!temporary.renameTo(destination)) error("Could not save the downloaded installer.")
            onProgress(1f)
            return destination.absolutePath
        } catch (error: Exception) {
            temporary.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun compareVersions(first: String, second: String): Int {
        val firstParts = versionParts(first)
        val secondParts = versionParts(second)
        val partCount = maxOf(firstParts.size, secondParts.size)
        for (index in 0 until partCount) {
            val left = firstParts.getOrElse(index) { 0 }
            val right = secondParts.getOrElse(index) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }

    private fun versionParts(version: String): List<Int> = version
        .substringBefore('-')
        .split('.')
        .mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }

    @Serializable
    private data class GithubRelease(
        @SerialName("tag_name") val tagName: String,
        val name: String? = null,
        val body: String? = null,
        val assets: List<GithubReleaseAsset> = emptyList()
    )

    @Serializable
    private data class GithubReleaseAsset(
        val name: String,
        @SerialName("browser_download_url") val downloadUrl: String,
        val size: Long = 0L
    )

    private companion object {
        const val RELEASE_API_URL = "https://api.github.com/repos/abzgif/SendChat/releases/latest"
        val releaseJson = Json { ignoreUnknownKeys = true }
    }
}
