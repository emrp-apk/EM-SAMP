package com.emrp.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

class ClientLauncher(private val context: Context) {
    fun findInstalledPackage(): String? {
        val pm = context.packageManager
        return LauncherConfig.CLIENT_PACKAGES.firstOrNull { pkg ->
            try { pm.getPackageInfo(pkg, 0); true } catch (_: PackageManager.NameNotFoundException) { false }
        }
    }

    fun launch(characterName: String, selectedPackage: String? = null): Result<Unit> {
        val pkg = selectedPackage?.takeIf { it.isNotBlank() } ?: findInstalledPackage() ?: return Result.failure(IllegalStateException("No supported S-MP mobile client is installed."))
        val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return Result.failure(IllegalStateException("Client launch activity not found."))
        // These extras are intentionally adapter-friendly. Different S-MP clients expose different keys.
        launch.putExtra("server", LauncherConfig.SERVER_HOST)
        launch.putExtra("host", LauncherConfig.SERVER_HOST)
        launch.putExtra("ip", LauncherConfig.SERVER_HOST)
        launch.putExtra("port", LauncherConfig.SERVER_PORT)
        launch.putExtra("nickname", characterName)
        launch.putExtra("name", characterName)
        launch.data = Uri.parse("samp://${LauncherConfig.SERVER_HOST}:${LauncherConfig.SERVER_PORT}")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        return Result.success(Unit)
    }
}
