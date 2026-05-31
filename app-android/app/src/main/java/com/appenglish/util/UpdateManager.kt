package com.appenglish.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.appenglish.data.remote.api.ContentApi
import com.appenglish.data.remote.api.TranslateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor() {

    private var checkingVersion = false

    fun checkForUpdate(activity: Activity) {
        if (checkingVersion) return
        checkingVersion = true

        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("${ApiConfig.BASE_URL}version")
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) { checkingVersion = false; return@Thread }

                val json = JSONObject(response.body?.string() ?: "")
                val serverVersion = json.optInt("versionCode", 0)
                val apkUrl = json.optString("apkUrl", "")

                val currentVersion = activity.packageManager
                    .getPackageInfo(activity.packageName, 0)
                    .versionCode

                if (serverVersion > currentVersion && apkUrl.isNotEmpty()) {
                    activity.runOnUiThread {
                        showUpdateDialog(activity, apkUrl, json.optString("versionName", ""))
                    }
                }
            } catch (_: Exception) { }
            finally { checkingVersion = false }
        }.start()
    }

    private fun showUpdateDialog(activity: Activity, apkUrl: String, versionName: String) {
        AlertDialog.Builder(activity)
            .setTitle("Nueva versión disponible")
            .setMessage("Versión $versionName está disponible. ¿Querés actualizar ahora?")
            .setPositiveButton("Actualizar") { _, _ ->
                downloadAndInstall(activity, apkUrl)
            }
            .setNegativeButton("Después", null)
            .show()
    }

    private fun downloadAndInstall(activity: Activity, apkUrl: String) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Descargando...")
            .setMessage("Esperá mientras se descarga la actualización.")
            .setCancelable(false)
            .create()
        dialog.show()

        Thread {
            try {
                val url = if (apkUrl.startsWith("http")) apkUrl
                    else "${ApiConfig.BASE_URL.removeSuffix("/api/v1/")}${apkUrl}"

                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) { activity.runOnUiThread { dialog.dismiss() }; return@Thread }

                val apkFile = File(activity.cacheDir, "update.apk")
                val bytes = response.body?.bytes()
                if (bytes == null) { activity.runOnUiThread { dialog.dismiss() }; return@Thread }
                FileOutputStream(apkFile).use { it.write(bytes) }

                activity.runOnUiThread {
                    dialog.dismiss()
                    installApk(activity, apkFile)
                }
            } catch (_: Exception) {
                activity.runOnUiThread { dialog.dismiss() }
            }
        }.start()
    }

    private fun installApk(context: Context, apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }
}
