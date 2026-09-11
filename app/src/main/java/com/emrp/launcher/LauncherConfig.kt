package com.emrp.launcher

object LauncherConfig {
    const val SERVER_NAME = "EM-SAMP"
    const val SERVER_HOST = "play.emrp.online"
    const val SERVER_PORT = 2026

    // Replace these with your real EMRP endpoints before release.
    const val MANIFEST_URL = "https://play.emrp.online/launcher/manifest.json"
    const val NEWS_URL = "https://play.emrp.online/launcher/news.json"
    const val FORUM_URL = "https://play.emrp.online/"
    const val UCP_URL = "https://play.emrp.online/ucp"
    const val DONATE_URL = "https://play.emrp.online/donate"
    const val DISCORD_URL = "https://discord.gg/REPLACE_ME"

    // Common SA-MP Android package IDs. Add the exact client package used by EMRP here.
    val CLIENT_PACKAGES = listOf(
        "com.unisamp.client",
        "com.cappuccino.samp",
        "ru.unisamp_mobile.launcher",
        "ro.alynsampmobile.launcher",
        "com.emrp.sampmobile"
    )
}
