package com.emrp.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT <= 32 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }
        setContent { EMSAMPApp() }
    }
}

@Composable
fun EMSAMPApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("em_samp", 0) }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(prefs.getString("character_name", "") ?: "") }
    var status by remember { mutableStateOf(ServerStatus()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var treeUri by remember { mutableStateOf(prefs.getString("game_tree", null)?.let(Uri::parse)) }
    var aimSensitivity by remember { mutableFloatStateOf(prefs.getFloat("aim_sensitivity", 0.55f)) }
    var crosshair by remember { mutableStateOf(prefs.getBoolean("crosshair", true)) }
    var showAim by remember { mutableStateOf(false) }
    var news by remember { mutableStateOf(listOf("Welcome to EM-SAMP. Set your character name and connect.")) }

    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            treeUri = uri
            prefs.edit().putString("game_tree", uri.toString()).apply()
            message = "Game / S-MP folder selected."
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            status = try { SampQueryClient().query(LauncherConfig.SERVER_HOST, LauncherConfig.SERVER_PORT) }
            catch (_: Exception) { ServerStatus() }
            delay(15_000)
        }
    }

    LaunchedEffect(Unit) { news = fetchNews() }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Color(0xFF07090D), surface = Color(0xFF10141B), primary = Color(0xFF19E6A1), secondary = Color(0xFF25A7FF)
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("EM-SAMP", fontSize = 32.sp, fontWeight = FontWeight.Black)
                            Text("Empire Mallu Roleplay", color = Color.LightGray)
                            Text("play.emrp.online:2026", color = Color.Gray, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(if (status.online) Color(0xFF19E6A1) else Color.Red, RoundedCornerShape(50)))
                            Spacer(Modifier.width(7.dp))
                            Text(if (status.online) "ONLINE" else "OFFLINE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("EMRP SERVER", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Metric("PLAYERS", "${status.players}/${status.maxPlayers}")
                                Metric("PING", status.pingMs?.let { "${it} ms" } ?: "—")
                                Metric("MODE", if (status.mode.isBlank()) "RP" else status.mode.take(10))
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it.replace(" ", "") }, singleLine = true,
                        label = { Text("RP Character Name") }, placeholder = { Text("Firstname_Lastname") },
                        supportingText = { Text(if (NameValidator.isValid(name)) "Valid roleplay name" else "Letters only: Firstname_Lastname") },
                        isError = name.isNotEmpty() && !NameValidator.isValid(name), modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(onClick = {
                        if (!NameValidator.isValid(name)) { message = "Enter a valid Firstname_Lastname first."; return@Button }
                        prefs.edit().putString("character_name", name).apply()
                        scope.launch {
                            busy = true
                            val result = ClientLauncher(context).launch(name, prefs.getString("selected_client", null))
                            busy = false
                            message = result.exceptionOrNull()?.message ?: "Launching S-MP client…"
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp)) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp))
                        Text(if (busy) "CONNECTING…" else "PLAY NOW • CONNECT TO EMRP", fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                }
                item {
                    var showClientMenu by remember { mutableStateOf(false) }
                    var selectedClient by remember {
                        mutableStateOf(prefs.getString("selected_client", LauncherConfig.CLIENT_PACKAGES.first()) ?: LauncherConfig.CLIENT_PACKAGES.first())
                    }

                    Box {
                        OutlinedButton(
                            onClick = { showClientMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, null)
                            Spacer(Modifier.width(8.dp))
                            Text("CLIENT • $selectedClient")
                        }

                        DropdownMenu(
                            expanded = showClientMenu,
                            onDismissRequest = { showClientMenu = false }
                        ) {
                            LauncherConfig.CLIENT_PACKAGES.forEach { client ->
                                DropdownMenuItem(
                                    text = { Text(client) },
                                    onClick = {
                                        selectedClient = client
                                        prefs.edit().putString("selected_client", client).apply()
                                        showClientMenu = false
                                        message = "Client selected: $client"
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = { treePicker.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp))
                        Text(if (treeUri == null) "SET GAME / S-MP FOLDER" else "GAME FOLDER SELECTED")
                    }
                }
                item {
                    OutlinedButton(onClick = { showAim = !showAim }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Tune, null); Spacer(Modifier.width(8.dp)); Text("AIM & TOUCH CONTROLS")
                    }
                }
                if (showAim) item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("AIM & TOUCH", fontWeight = FontWeight.Bold)
                            Text("Fair control settings for the mobile client. This launcher does not modify game memory or add automatic targeting.", color = Color.Gray, fontSize = 12.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("Aim sensitivity: ${(aimSensitivity * 100).toInt()}%")
                            Slider(value = aimSensitivity, onValueChange = {
                                aimSensitivity = it
                                prefs.edit().putFloat("aim_sensitivity", it).apply()
                            }, valueRange = 0.1f..1f)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = crosshair, onCheckedChange = {
                                    crosshair = it
                                    prefs.edit().putBoolean("crosshair", it).apply()
                                })
                                Spacer(Modifier.width(8.dp)); Text("Show crosshair")
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickButton("UCP", Icons.Default.Person) { openUrl(context, LauncherConfig.UCP_URL) }
                        QuickButton("FORUM", Icons.Default.Public) { openUrl(context, LauncherConfig.FORUM_URL) }
                        QuickButton("STORE", Icons.Default.ShoppingCart) { openUrl(context, LauncherConfig.DONATE_URL) }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("EMRP NEWS", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            news.take(5).forEach { Text("• $it", color = Color.LightGray, modifier = Modifier.padding(vertical = 3.dp)) }
                        }
                    }
                }
                if (message != null) item { Text(message!!, color = MaterialTheme.colorScheme.secondary) }
            }
        }
    }
}

@Composable private fun Metric(label: String, value: String) {
    Column { Text(label, color = Color.Gray, fontSize = 10.sp); Text(value, fontWeight = FontWeight.Bold) }
}

@Composable private fun RowScope.QuickButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) {
    OutlinedButton(onClick = action, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
        Icon(icon, null); Spacer(Modifier.width(4.dp)); Text(label, fontSize = 11.sp)
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) { }
}

private suspend fun fetchNews(): List<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val conn = URL(LauncherConfig.NEWS_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 6000; conn.readTimeout = 6000
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val arr = JSONArray(text)
        (0 until minOf(arr.length(), 10)).map { i ->
            val item = arr.get(i)
            if (item is JSONObject) item.optString("title", "Announcement") else item.toString()
        }
    } catch (_: Exception) { listOf("News feed unavailable right now.") }
}
