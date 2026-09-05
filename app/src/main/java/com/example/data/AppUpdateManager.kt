package com.example.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ReleaseUpdateInfo(
  val tagName: String,
  val releaseName: String,
  val releaseNotes: String,
  val apkDownloadUrl: String,
  val apkFileName: String,
  val apkSizeBytes: Long,
  val isUpdateAvailable: Boolean
)

sealed class UpdateCheckState {
  object Idle : UpdateCheckState()
  object Checking : UpdateCheckState()
  data class UpToDate(val currentVersion: String) : UpdateCheckState()
  data class Available(val updateInfo: ReleaseUpdateInfo) : UpdateCheckState()
  data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateCheckState()
  data class ReadyToInstall(val apkFile: File) : UpdateCheckState()
  data class Error(val message: String) : UpdateCheckState()
}

class AppUpdateManager(private val context: Context) {

  companion object {
    const val GITHUB_OWNER = "Moksh-05"
    const val GITHUB_REPO = "JIIM"
    const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
  }

  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(180, TimeUnit.SECONDS)
    .writeTimeout(180, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

  val currentVersionName: String
    get() = try {
      BuildConfig.VERSION_NAME
    } catch (_: Throwable) {
      "1.0"
    }

  val currentVersionCode: Int
    get() = try {
      BuildConfig.VERSION_CODE
    } catch (_: Throwable) {
      1
    }

  suspend fun checkForUpdates(): ReleaseUpdateInfo? = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url(API_URL)
        .header("Accept", "application/vnd.github.v3+json")
        .header("User-Agent", "JIIM-Android-App")
        .build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          Log.w("AppUpdateManager", "GitHub release check failed: HTTP ${response.code}")
          return@withContext null
        }

        val jsonStr = response.body?.string() ?: return@withContext null
        val root = JSONObject(jsonStr)

        val tagName = root.optString("tag_name", "").trim()
        val releaseName = root.optString("name", tagName)
        val releaseNotes = root.optString("body", "No changelog provided.")

        val assets = root.optJSONArray("assets") ?: return@withContext null
        var apkDownloadUrl = ""
        var apkFileName = ""
        var apkSizeBytes = 0L

        // Look for .apk asset
        for (i in 0 until assets.length()) {
          val asset = assets.getJSONObject(i)
          val name = asset.optString("name", "")
          if (name.endsWith(".apk", ignoreCase = true)) {
            apkDownloadUrl = asset.optString("browser_download_url", "")
            apkFileName = name
            apkSizeBytes = asset.optLong("size", 0L)
            break
          }
        }

        // If no specifically named .apk found, take the first asset if available
        if (apkDownloadUrl.isEmpty() && assets.length() > 0) {
          val asset = assets.getJSONObject(0)
          apkDownloadUrl = asset.optString("browser_download_url", "")
          apkFileName = asset.optString("name", "jiim-update.apk")
          apkSizeBytes = asset.optLong("size", 0L)
        }

        if (apkDownloadUrl.isEmpty()) {
          Log.w("AppUpdateManager", "No APK asset found in latest GitHub release")
          return@withContext null
        }

        val hasUpdate = isNewerVersion(tagName, currentVersionName)

        ReleaseUpdateInfo(
          tagName = tagName,
          releaseName = releaseName,
          releaseNotes = releaseNotes,
          apkDownloadUrl = apkDownloadUrl,
          apkFileName = apkFileName,
          apkSizeBytes = apkSizeBytes,
          isUpdateAvailable = hasUpdate
        )
      }
    } catch (e: Exception) {
      Log.e("AppUpdateManager", "Error checking for updates on GitHub", e)
      null
    }
  }

  fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
    val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
    val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

    if (cleanRemote.isBlank()) return false
    if (cleanRemote == cleanCurrent) return false

    val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
    val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

    val maxLen = maxOf(remoteParts.size, currentParts.size)
    for (i in 0 until maxLen) {
      val r = remoteParts.getOrElse(i) { 0 }
      val c = currentParts.getOrElse(i) { 0 }
      if (r > c) return true
      if (r < c) return false
    }

    return cleanRemote != cleanCurrent
  }

  fun canInstallPackages(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.packageManager.canRequestPackageInstalls()
    } else {
      true
    }
  }

  fun openInstallPermissionSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(settingsIntent)
    }
  }

  fun openDownloadInBrowser(downloadUrl: String) {
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e("AppUpdateManager", "Error opening download in browser", e)
    }
  }

  suspend fun downloadApk(
    downloadUrl: String,
    onProgress: (Int, Long, Long) -> Unit
  ): File? = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url(downloadUrl)
        .header("User-Agent", "JIIM-Android-App")
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        Log.e("AppUpdateManager", "Failed to download APK: HTTP ${response.code}")
        return@withContext null
      }

      val body = response.body ?: return@withContext null
      val totalBytes = body.contentLength()

      val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
      val targetFile = File(updatesDir, "JIIM_update.apk")
      if (targetFile.exists()) {
        targetFile.delete()
      }

      body.byteStream().use { input ->
        FileOutputStream(targetFile).use { output ->
          val buffer = ByteArray(8192)
          var read: Int
          var totalRead = 0L

          while (input.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
            totalRead += read
            val percent = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt() else -1
            onProgress(percent, totalRead, totalBytes)
          }
          output.flush()
        }
      }

      targetFile.setReadable(true, false)
      targetFile
    } catch (e: Exception) {
      Log.e("AppUpdateManager", "Error downloading APK from GitHub", e)
      null
    }
  }

  fun installApk(apkFile: File): Boolean {
    return try {
      if (!apkFile.exists()) return false

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
          openInstallPermissionSettings()
          return false
        }
      }

      val apkUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
      )

      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
      }

      val resolveInfoList = context.packageManager.queryIntentActivities(
        intent,
        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
      )
      for (resolveInfo in resolveInfoList) {
        val packageName = resolveInfo.activityInfo.packageName
        context.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      context.startActivity(intent)
      true
    } catch (e: Exception) {
      Log.e("AppUpdateManager", "Error launching APK installer", e)
      false
    }
  }
}
