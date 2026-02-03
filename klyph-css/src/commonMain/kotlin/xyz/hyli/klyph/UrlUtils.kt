/*
 * Copyright 2026 Klyph Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.hyli.klyph

import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastJoinToString
import io.ktor.util.*

/**
 * Resolves a potentially relative URL against a base URL.
 *
 * @param baseUrl The base URL (typically the CSS file URL).
 * @param relativeUrl The URL to resolve (may be relative or absolute).
 * @return The resolved absolute URL.
 */
internal fun resolveUrl(baseUrl: String, relativeUrl: String): String {
    // If the URL is already absolute (has protocol), return as-is
    if (relativeUrl.startsWith("http://") ||
        relativeUrl.startsWith("https://") ||
        relativeUrl.startsWith("//") ||
        relativeUrl.startsWith("data:")) {
        return relativeUrl
    }

    // Parse the base URL
    val baseUrlParts = parseUrl(baseUrl)

    // Handle protocol-relative URLs (//example.com/font.woff2)
    if (relativeUrl.startsWith("//")) {
        return "${baseUrlParts.protocol}:$relativeUrl"
    }

    // Handle absolute paths (/fonts/font.woff2)
    if (relativeUrl.startsWith("/")) {
        return "${baseUrlParts.protocol}://${baseUrlParts.host}$relativeUrl"
    }

    // Handle relative paths (./font.woff2, ../fonts/font.woff2, font.woff2)
    val basePath = baseUrlParts.path.substringBeforeLast('/', "")
    val resolvedPath = resolveRelativePath(basePath, relativeUrl)

    return "${baseUrlParts.protocol}://${baseUrlParts.host}$resolvedPath"
}

/**
 * Decodes a data URL into raw bytes.
 *
 * Supports base64 and URL-encoded payloads.
 */
internal fun decodeDataUrlToBytes(url: String): ByteArray {
    val commaIndex = url.indexOf(',')
    if (commaIndex == -1) {
        return ByteArray(0)
    }
    val header = url.substring(5, commaIndex)
    val dataPart = url.substring(commaIndex + 1)
    val isBase64 = header.split(';').any { it.equals("base64", true) }
    return if (isBase64) {
        dataPart.replace(Regex("\\s+"), "").decodeBase64Bytes()
    } else {
        decodeUrlEncodedBytes(dataPart)
    }
}

/**
 * Parses a URL into its components.
 *
 * @param url The URL to parse.
 * @return A UrlParts object containing the protocol, host, and path.
 */
private fun parseUrl(url: String): UrlParts {
    // Extract protocol
    val protocolEnd = url.indexOf("://")
    if (protocolEnd == -1) {
        // No protocol, treat as relative - shouldn't happen for base URLs
        return UrlParts("https", "", url)
    }

    val protocol = url.substring(0, protocolEnd)
    val afterProtocol = url.substring(protocolEnd + 3)

    // Extract host and path
    val pathStart = afterProtocol.indexOf('/')
    val (host, path) = if (pathStart == -1) {
        // No path, just host
        afterProtocol to ""
    } else {
        afterProtocol.substring(0, pathStart) to afterProtocol.substring(pathStart)
    }

    return UrlParts(protocol, host, path)
}

/**
 * Resolves a relative path against a base path.
 *
 * @param basePath The base path (e.g., "/styles").
 * @param relativePath The relative path (e.g., "./font.woff2" or "../fonts/font.woff2").
 * @return The resolved absolute path.
 */
private fun resolveRelativePath(basePath: String, relativePath: String): String {
    // Split paths into segments
    val baseSegments = basePath.split('/').fastFilter { it.isNotEmpty() }.toMutableList()
    val relativeSegments = relativePath.split('/').fastFilter { it.isNotEmpty() }

    for (segment in relativeSegments) {
        when (segment) {
            "." -> {
                // Current directory, do nothing
            }
            ".." -> {
                // Parent directory, go up one level
                if (baseSegments.isNotEmpty()) {
                    baseSegments.removeLast()
                }
            }
            else -> {
                // Regular segment, add it
                baseSegments.add(segment)
            }
        }
    }

    return "/" + baseSegments.fastJoinToString("/")
}

private fun decodeUrlEncodedBytes(value: String): ByteArray {
    val output = ByteArray(value.length)
    var outIndex = 0
    var i = 0
    while (i < value.length) {
        val ch = value[i]
        if (ch == '%' && i + 2 < value.length) {
            val hi = hexValue(value[i + 1])
            val lo = hexValue(value[i + 2])
            if (hi >= 0 && lo >= 0) {
                output[outIndex++] = ((hi shl 4) + lo).toByte()
                i += 3
                continue
            }
        }
        output[outIndex++] = ch.code.toByte()
        i += 1
    }
    return output.copyOf(outIndex)
}

private fun hexValue(ch: Char): Int = when (ch) {
    in '0'..'9' -> ch.code - '0'.code
    in 'a'..'f' -> ch.code - 'a'.code + 10
    in 'A'..'F' -> ch.code - 'A'.code + 10
    else -> -1
}

/**
 * Data class representing the parts of a URL.
 */
private data class UrlParts(
    val protocol: String,
    val host: String,
    val path: String
)
