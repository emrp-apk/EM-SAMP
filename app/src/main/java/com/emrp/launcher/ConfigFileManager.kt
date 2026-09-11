package com.emrp.launcher

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ConfigFileManager(private val context: Context) {
    fun writeNameToTree(treeUri: android.net.Uri, characterName: String): Boolean {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        val file = findSettings(tree) ?: return false
        val input = context.contentResolver.openInputStream(file.uri) ?: return false
        val lines = BufferedReader(InputStreamReader(input)).use { it.readLines().toMutableList() }
        input.close()
        var found = false
        for (i in lines.indices) {
            if (lines[i].trimStart().startsWith("name", ignoreCase = true)) {
                lines[i] = "name = $characterName"
                found = true
                break
            }
        }
        if (!found) lines.add("name = $characterName")
        val output = context.contentResolver.openOutputStream(file.uri, "wt") ?: return false
        OutputStreamWriter(output).use { it.write(lines.joinToString("\n")) }
        return true
    }

    private fun findSettings(root: DocumentFile): DocumentFile? {
        root.findFile("settings.ini")?.let { return it }
        root.findFile("SAMP")?.findFile("settings.ini")?.let { return it }
        root.findFile("samp")?.findFile("settings.ini")?.let { return it }
        return null
    }
}
