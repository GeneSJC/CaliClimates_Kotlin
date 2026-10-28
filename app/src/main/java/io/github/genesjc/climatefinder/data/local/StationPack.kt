package io.github.genesjc.climatefinder.data.local

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream

/**
 * Reads a station pack: a region's station rows, compressed and lightly scrambled, stored as one line
 * of text (a.txt to f.txt). The pack is obscured, not secret: it keeps casual readers out, and the
 * checks below make a damaged file get noticed instead of being read as wrong data.
 *
 * Layout (version 1), Base64 text of:
 *   "MCSP" | version byte | CRC32 of the stored bytes (4 bytes, big-endian) | stored bytes
 * where stored bytes = gzip(CSV text, UTF-8) XOR KEY (repeating).
 *
 * Packs are made from the CSV files by the converter script kept with the project's internal notes.
 * Its KEY and this layout must stay in step; bump VERSION if either changes.
 */
object StationPack {

    private const val VERSION = 1
    private const val HEADER_SIZE = 9
    private val MAGIC = byteArrayOf('M'.code.toByte(), 'C'.code.toByte(), 'S'.code.toByte(), 'P'.code.toByte())
    private val KEY = intArrayOf(
        0x5A, 0xC3, 0x17, 0x8E, 0x2D, 0xB4, 0x61, 0xF9,
        0x0C, 0x73, 0xD8, 0x4E, 0xA5, 0x39, 0x96, 0x1B
    )
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    /**
     * The CSV text inside [packText], or null when it is not a pack, is damaged (bad checksum or
     * data), or has a format version this app does not know.
     */
    fun decode(packText: String): String? = try {
        decodeChecked(packText)
    } catch (e: Exception) {
        null
    }

    private fun decodeChecked(packText: String): String? {
        val bytes = base64Decode(packText) ?: return null
        if (bytes.size <= HEADER_SIZE) return null
        for (i in MAGIC.indices) if (bytes[i] != MAGIC[i]) return null
        if (bytes[4].toInt() != VERSION) return null

        var expectedCrc = 0L
        for (i in 5..8) expectedCrc = (expectedCrc shl 8) or (bytes[i].toLong() and 0xFF)
        val stored = bytes.copyOfRange(HEADER_SIZE, bytes.size)
        val crc = CRC32().apply { update(stored) }
        if (crc.value != expectedCrc) return null

        val compressed = ByteArray(stored.size) { i -> (stored[i].toInt() xor KEY[i % KEY.size]).toByte() }
        val csv = GZIPInputStream(ByteArrayInputStream(compressed)).use { it.readBytes() }
        return String(csv, Charsets.UTF_8)
    }

    // Own Base64 reader (java.util.Base64 needs API 26, this app supports 24, and this keeps it
    // testable off-device). Ignores whitespace and line wraps; stops at '=' padding; null on any other character.
    private fun base64Decode(text: String): ByteArray? {
        val out = ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (ch in text) {
            if (ch == '=') break
            if (ch.isWhitespace()) continue
            val value = ALPHABET.indexOf(ch)
            if (value < 0) return null
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((buffer shr bits) and 0xFF)
                buffer = buffer and ((1 shl bits) - 1)
            }
        }
        return out.toByteArray()
    }
}
