package com.example.jellyfintv.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.jellyfintv.data.api.GithubApi
import com.example.jellyfintv.data.api.GithubClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String?,
    val downloadUrl: String,
    val apkSizeBytes: Long
)

class AppUpdateManager(
    private val githubApi: GithubApi = GithubClient.getApi(),
    private val downloadClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    suspend fun checkForUpdate(currentVersionName: String): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val response = githubApi.getLatestRelease(REPO_OWNER, REPO_NAME)
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub returned HTTP ${response.code()}"))
            }
            val release = response.body() ?: return@withContext Result.success(null)
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: return@withContext Result.success(null) // no installable asset published yet

            if (!isNewerVersion(currentVersionName, release.tagName)) {
                return@withContext Result.success(null)
            }

            Result.success(
                UpdateInfo(
                    versionName = release.tagName,
                    releaseNotes = release.body,
                    downloadUrl = apkAsset.browserDownloadUrl,
                    apkSizeBytes = apkAsset.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download failed: HTTP ${response.code}")
            val body = response.body
            val totalBytes = body.contentLength()

            val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
            val outputFile = File(outputDir, "update.apk")

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        onProgress(bytesRead, totalBytes)
                    }
                }
            }
            outputFile
        }
    }

    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun canRequestPackageInstalls(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** Launches the system settings screen where the user grants "install unknown apps" for this app. */
    fun installPermissionSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    fun currentVersionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0"
    } catch (e: PackageManager.NameNotFoundException) {
        "0.0"
    }

    companion object {
        const val REPO_OWNER = "rpeters1430"
        const val REPO_NAME = "Jellyfin-TV"

        /**
         * Compares dotted version strings (tolerating a leading "v" and a "-beta"/"+build"
         * suffix, both common in GitHub tag names) numeric-segment by numeric-segment, so
         * "1.10.0" correctly beats "1.9.0" (a plain string compare would get that backwards).
         */
        fun isNewerVersion(current: String, latest: String): Boolean {
            val currentParts = normalize(current)
            val latestParts = normalize(latest)
            val maxLen = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLen) {
                val c = currentParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l != c) return l > c
            }
            return false
        }

        private fun normalize(version: String): List<Int> =
            version.trim()
                .removePrefix("v").removePrefix("V")
                .substringBefore("-")
                .substringBefore("+")
                .split(".")
                .map { it.toIntOrNull() ?: 0 }
    }
}
