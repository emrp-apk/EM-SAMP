package com.emrp.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.roundToLong

class SampQueryClient {
    suspend fun query(host: String, port: Int, timeoutMs: Int = 1800): ServerStatus = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMs
            val address = InetAddress.getByName(host)
            val ip = address.address
            val packet = ByteArray(11)
            packet[0] = 'S'.code.toByte(); packet[1] = 'A'.code.toByte(); packet[2] = 'M'.code.toByte(); packet[3] = 'P'.code.toByte()
            System.arraycopy(ip, 0, packet, 4, 4)
            packet[8] = (port and 0xFF).toByte(); packet[9] = ((port shr 8) and 0xFF).toByte()
            packet[10] = 'i'.code.toByte()
            socket.send(DatagramPacket(packet, packet.size, address, port))

            val response = ByteArray(4096)
            val reply = DatagramPacket(response, response.size)
            socket.receive(reply)
            val ping = ((System.nanoTime() - start) / 1_000_000.0).roundToLong()
            parseInfo(response.copyOf(reply.length), ping)
        }
    }

    private fun parseInfo(bytes: ByteArray, ping: Long): ServerStatus {
        // Response begins with SAMP + echoed IPv4 + port + 'i'.
        if (bytes.size < 14 || bytes[0].toInt().toChar() != 'S') return ServerStatus(online = true, pingMs = ping)
        var p = 11
        if (bytes[p++].toInt().toChar() != 'i') return ServerStatus(online = true, pingMs = ping)
        p++ // password flag
        val players = u16(bytes, p); p += 2
        val max = u16(bytes, p); p += 2
        val host = readString(bytes, p).also { p += 1 + it.second }.first
        val mode = readString(bytes, p).first
        return ServerStatus(true, players, max, ping, host, mode)
    }

    private fun u16(b: ByteArray, p: Int): Int = (b[p].toInt() and 0xff) or ((b[p + 1].toInt() and 0xff) shl 8)
    private fun readString(b: ByteArray, start: Int): Pair<String, Int> {
        if (start + 4 > b.size) return "" to 0
        val len = (b[start].toInt() and 0xff) or ((b[start + 1].toInt() and 0xff) shl 8) or ((b[start + 2].toInt() and 0xff) shl 16) or ((b[start + 3].toInt() and 0xff) shl 24)
        val end = (start + 4 + len).coerceAtMost(b.size)
        return b.copyOfRange(start + 4, end).toString(Charsets.UTF_8) to (4 + len)
    }
}
