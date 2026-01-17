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

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font as PlatformFont
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Global cache for loaded font slices.
 * Maps font URLs to their loaded ByteArray data.
 */
object FontSliceCache {
    private val cache = mutableMapOf<String, ByteArray>()
    private val mutex = Mutex()

    /**
     * Gets font data from cache or loads it from the URL if not cached.
     *
     * @param url The URL of the font resource.
     * @return The font data as ByteArray.
     */
    suspend fun getOrLoad(url: String): ByteArray {
        // Check cache first (read lock)
        mutex.withLock {
            cache[url]?.let { return it }
        }

        // Not in cache, load the font
        val fontData = getFontData(url)

        // Store in cache (write lock)
        mutex.withLock {
            cache[url] = fontData
        }

        return fontData
    }

    /**
     * Preloads multiple font URLs into the cache.
     *
     * @param urls The list of URLs to preload.
     */
    suspend fun preload(urls: List<String>) {
        urls.forEach { url ->
            try {
                getOrLoad(url)
            } catch (e: Exception) {
                println("ERROR: Failed to preload font from $url: ${e.message}")
            }
        }
    }

    /**
     * Clears all cached font data.
     */
    suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }

    /**
     * Gets the current cache size.
     *
     * @return The number of cached font slices.
     */
    suspend fun size(): Int {
        return mutex.withLock {
            cache.size
        }
    }
}

/**
 * Represents a CSS font descriptor with parsed metadata.
 */
data class ParsedFontDescriptor(
    val url: String,
    val fontFamily: String,
    val weight: FontWeight,
    val style: FontStyle,
    val unicodeRanges: List<UnicodeRange>
)

/**
 * Parses a FontFace into a ParsedFontDescriptor with typed properties.
 *
 * @param fontFace The FontFace to parse.
 * @return A ParsedFontDescriptor or null if the font face cannot be used.
 */
fun parseFontDescriptor(fontFace: FontFace): ParsedFontDescriptor? {
    // Get the first URL source (skip local fonts for web compatibility)
    val url = fontFace.src.firstOrNull { it.url != null }?.url ?: return null

    // Parse font weight
    val weight = parseFontWeight(fontFace.fontWeight)

    // Parse font style
    val style = parseFontStyle(fontFace.fontStyle)

    // Parse unicode ranges
    val unicodeRanges = parseUnicodeRange(fontFace.unicodeRange)

    return ParsedFontDescriptor(
        url = url,
        fontFamily = fontFace.fontFamily,
        weight = weight,
        style = style,
        unicodeRanges = unicodeRanges
    )
}

/**
 * Parses a CSS font-weight value into a FontWeight.
 *
 * @param weightStr The font-weight value from CSS (e.g., "normal", "bold", "400").
 * @return The corresponding FontWeight.
 */
private fun parseFontWeight(weightStr: String?): FontWeight {
    return when (weightStr?.lowercase()) {
        "normal" -> FontWeight.Normal
        "bold" -> FontWeight.Bold
        else -> {
            // Try parsing as integer
            weightStr?.toIntOrNull()?.let {
                FontWeight(it)
            } ?: FontWeight.Normal
        }
    }
}

/**
 * Parses a CSS font-style value into a FontStyle.
 *
 * @param styleStr The font-style value from CSS (e.g., "normal", "italic").
 * @return The corresponding FontStyle.
 */
private fun parseFontStyle(styleStr: String?): FontStyle {
    return when (styleStr?.lowercase()) {
        "italic", "oblique" -> FontStyle.Italic
        else -> FontStyle.Normal
    }
}

/**
 * Creates a Font instance from loaded font data.
 *
 * @param data The font data as ByteArray.
 * @param descriptor The parsed font descriptor with metadata.
 * @return A Font instance.
 */
fun createFontFromData(data: ByteArray, descriptor: ParsedFontDescriptor): Font {
    return PlatformFont(
        identity = "${descriptor.fontFamily}-${descriptor.weight.weight}-${descriptor.style}-${descriptor.url}",
        data = data,
        weight = descriptor.weight,
        style = descriptor.style
    )
}
