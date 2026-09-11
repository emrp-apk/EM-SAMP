package com.emrp.launcher

data class ServerStatus(
    val online: Boolean = false,
    val players: Int = 0,
    val maxPlayers: Int = 0,
    val pingMs: Long? = null,
    val hostname: String = "",
    val mode: String = ""
)

data class ManifestAsset(
    val path: String,
    val url: String,
    val sha256: String? = null,
    val size: Long? = null
)

data class AssetManifest(val version: Int = 1, val assets: List<ManifestAsset> = emptyList())
