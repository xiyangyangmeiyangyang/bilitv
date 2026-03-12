package com.openclaw.bilitv.data.repository

import java.security.MessageDigest
import java.net.URLEncoder
import java.util.Locale

internal object BiliWbi {
    private val mixinKeyEncTab = listOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    )

    fun sign(params: Map<String, Any>, imgKey: String, subKey: String, timestamp: Long = System.currentTimeMillis() / 1000): Map<String, String> {
        val mixinKey = getMixinKey(imgKey + subKey)
        val signed = params.mapValues { sanitizeValue(it.value.toString()) }.toMutableMap()
        signed["wts"] = timestamp.toString()
        val sorted = signed.toSortedMap()
        val query = sorted.entries.joinToString("&") { "${it.key}=${encodeComponent(it.value)}" }
        val wRid = md5(query + mixinKey)
        return buildMap {
            putAll(sorted)
            put("w_rid", wRid)
        }
    }

    private fun getMixinKey(orig: String): String {
        if (orig.length < 64) return orig
        return mixinKeyEncTab.map { orig[it] }.joinToString(separator = "").take(32)
    }

    private fun sanitizeValue(value: String): String {
        return value.filterNot { it in "!'()*" }
    }

    private fun encodeComponent(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
    }

    private fun md5(value: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(Locale.US, it) }
    }
}
