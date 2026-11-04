package com.henriquesebastiao.prumo.core.model

import java.net.URI
import java.net.URISyntaxException

/**
 * What the user typed as the server's address, turned into base URLs to try, in order. The user
 * types it "as in the browser": `prumo.minhacasa.net`, `192.168.0.10:8000`,
 * `https://prumo.exemplo.com/`, maybe with a path when the server lives under one. Without a
 * scheme, a local host (an IP of a private range, `localhost`, `*.local`, a name without dots) is
 * tried on `http://` first and anything else on `https://` first; the other scheme comes next. The
 * base URL has no trailing slash and never `/api`: the app adds `/api/...` itself.
 */
object ServerAddress {
    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    private const val HTTP = "http"
    private const val HTTPS = "https"

    /** The base URLs to try for [input], or an empty list when it can't be an address. */
    fun candidates(input: String): List<String> {
        val text = input.trim()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return emptyList()
        if (SCHEME.containsMatchIn(text)) {
            return listOfNotNull(normalize(text))
        }
        val http = normalize("$HTTP://$text") ?: return emptyList()
        val https = normalize("$HTTPS://$text") ?: return emptyList()
        return if (isLocal(hostOf(http))) listOf(http, https) else listOf(https, http)
    }

    /** [baseUrl] as shown to the user: the host, the port and the path, without the scheme. */
    fun display(baseUrl: String): String = baseUrl.substringAfter("://").trimEnd('/')

    /** Whether [baseUrl] is reached over plain HTTP on a host outside the home network (D14). */
    fun isInsecurePublic(baseUrl: String): Boolean = baseUrl.startsWith("$HTTP://") && !isLocal(hostOf(baseUrl))

    /** Whether [host] is on the home network: a private or loopback IP, `localhost` or `*.local`. */
    fun isLocal(host: String): Boolean {
        val name = host.lowercase().trim('[', ']')
        return isLocalName(name) || isPrivateIpv4(name) || isPrivateIpv6(name)
    }

    // "nas", "prumo": a name on the LAN, without dots.
    private fun isLocalName(name: String): Boolean = (!name.contains('.') && !name.contains(':')) ||
        name == "localhost" ||
        LOCAL_SUFFIXES.any { name.endsWith(it) }

    private fun isPrivateIpv4(name: String): Boolean {
        val parts = name.split('.').map { it.toIntOrNull() ?: return false }
        if (parts.size != IPV4_PARTS || parts.any { it !in 0..MAX_OCTET }) return false
        val (a, b) = parts
        return a == 10 || a == LOOPBACK || (a == 172 && b in 16..31) || (a == 192 && b == 168) ||
            (a == 169 && b == 254) || (a == 100 && b in 64..127)
    }

    private fun isPrivateIpv6(name: String): Boolean = name == "::1" || name.startsWith("fd") || name.startsWith("fe80")

    private fun normalize(text: String): String? {
        val uri = try {
            URI(text.trimEnd('/'))
        } catch (_: URISyntaxException) {
            return null
        }
        val scheme = uri.scheme?.lowercase()
        val host = uri.host
        val supported = scheme == HTTP || scheme == HTTPS
        val plain = uri.rawQuery == null && uri.rawFragment == null
        if (!supported || !plain || host.isNullOrEmpty()) return null
        val port = if (uri.port == -1) "" else ":${uri.port}"
        val path = uri.rawPath.orEmpty().trimEnd('/').removeSuffix("/api")
        return "$scheme://${host.lowercase()}$port$path"
    }

    private fun hostOf(baseUrl: String): String = URI(baseUrl).host.orEmpty()

    private val LOCAL_SUFFIXES = listOf(".local", ".lan", ".home.arpa")
    private const val IPV4_PARTS = 4
    private const val MAX_OCTET = 255
    private const val LOOPBACK = 127
}
