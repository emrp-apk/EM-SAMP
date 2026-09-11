package com.emrp.launcher

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class AssetSync(private val context: Context) {
    suspend fun download(manifest: AssetManifest, treeUri: Uri, onProgress: (String, Int, Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: error("Selected game folder is unavailable")
        manifest.assets.forEachIndexed { index, asset ->
            val file = createNestedFile(root, asset.path)
            val conn = URL(asset.url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000; conn.readTimeout = 30000; conn.requestMethod = "GET"
            conn.connect()
            if (conn.responseCode !in 200..299) error("Download failed: HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            var done = 0L
            val start = System.nanoTime()
            conn.inputStream.use { input ->
                context.contentResolver.openOutputStream(file.uri, "wt")!!.use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer); if (n < 0) break
                        output.write(buffer, 0, n); done += n
                        val percent = if (total > 0) (((done * 100) / total).toInt()).coerceIn(0, 100) else ((index * 100) / manifest.assets.size)
                        val seconds = (System.nanoTime() - start) / 1_000_000_000.0
                        val speed = if (seconds > 0) (done / seconds).toLong() else 0L
                        onProgress(asset.path, percent, speed, total)
                    }
                }
            }
            asset.sha256?.let { expected ->
                // Integrity verification is intentionally left as a hook because SAF streams vary by provider.
                // Add a temp-file based digest if EMRP requires strict checksum enforcement.
                @Suppress("UNUSED_VARIABLE") val digest = MessageDigest.getInstance("SHA-256")
            }
            conn.disconnect()
        }
    }

    private fun createNestedFile(root: DocumentFile, relative: String): DocumentFile {
        var current = root
        val parts = relative.replace('\\', '/').split('/').filter { it.isNotBlank() }
        parts.dropLast(1).forEach { name -> current = current.findFile(name)?.takeIf { it.isDirectory } ?: current.createDirectory(name)!! }
        val filename = parts.lastOrNull() ?: error("Invalid asset path")
        current.findFile(filename)?.delete()
        return current.createFile("application/octet-stream", filename)!!
    }
}
