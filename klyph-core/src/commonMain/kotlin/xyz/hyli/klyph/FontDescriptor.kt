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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

/**
 * Font descriptor for loading fonts from Compose resources.
 *
 * This implementation loads font data from local Compose resources,
 * making it suitable for bundled fonts shipped with the application.
 *
 * @property resource The font resource reference.
 * @property fontFamily The name of the font family.
 * @property weight The font weight (e.g., Normal, Bold, or custom weight).
 * @property style The font style (Normal or Italic).
 * @property unicodeRanges The list of Unicode ranges this font slice covers.
 */
data class ResourceFontDescriptor(
    val resource: FontResource,
    override val fontFamily: String,
    override val weight: FontWeight,
    override val style: FontStyle,
    override val unicodeRanges: List<UnicodeRange>
) : FontDescriptor {
    /**
     * Convenience constructor that accepts CSS-style strings for weight, style, and unicode ranges.
     *
     * This mirrors CSS @font-face fields, so you can pass values like:
     * - weight: "normal", "bold", "400", "700"
     * - style: "normal", "italic"
     * - unicodeRanges: "U+0-FF, U+131, U+152-153"
     *
     * @param resource The font resource reference.
     * @param fontFamily The name of the font family.
     * @param weight CSS-style font-weight string.
     * @param style CSS-style font-style string.
     * @param unicodeRanges CSS unicode-range string (single or comma-separated ranges). This is
     * passed directly to [parseUnicodeRange].
     */
    constructor(
        resource: FontResource,
        fontFamily: String,
        weight: String,
        style: String,
        unicodeRanges: String
    ) : this(
        resource = resource,
        fontFamily = fontFamily,
        weight = parseFontWeight(weight),
        style = parseFontStyle(style),
        unicodeRanges = parseUnicodeRange(unicodeRanges)
    )

    override val cacheKey: String
        get() = "hash:" + "${resource}:${weight}:${style}:${unicodeRanges}".toFnv1aHashString()

    override suspend fun getFontFamily(
        onBytesLoaded: (Long) -> Unit
    ): FontFamily {
        val env = getSystemResourceEnvironment()
        val fontData = getFontResourceBytes(env, resource)
        onBytesLoaded(fontData.size.toLong())
        return createFontFamilyFromData(fontData, this)
    }
}

/**
 * Interface for font descriptors that can load and provide font data.
 *
 * Implementations of this interface represent different sources of font data
 * and provide the metadata and loading logic needed to create font slices.
 *
 * @property cacheKey Unique identifier for caching this font descriptor.
 * @property fontFamily The name of the font family.
 * @property weight The font weight (e.g., Normal, Bold, or custom weight).
 * @property style The font style (Normal or Italic).
 * @property unicodeRanges The list of Unicode ranges this font slice covers.
 */
interface FontDescriptor {
    val cacheKey: String
    val fontFamily: String
    val weight: FontWeight
    val style: FontStyle
    val unicodeRanges: List<UnicodeRange>

    /**
     * Loads the font data and creates a Compose FontFamily instance.
     *
     * @param onBytesLoaded Callback invoked with the number of bytes loaded (for tracking/monitoring).
     * @return A Compose FontFamily instance ready to be used for text rendering.
     */
    suspend fun getFontFamily(
        onBytesLoaded: (Long) -> Unit = { }
    ): FontFamily
}

/**
 * Creates a Compose FontFamily instance from loaded font data.
 *
 * This function wraps the raw font ByteArray in a platform-specific Font object
 * with the appropriate metadata (weight, style, identity).
 *
 * @param data The font data as ByteArray.
 * @param descriptor The parsed font descriptor with metadata.
 * @return A Compose FontFamily instance ready to be used for text rendering.
 */
fun createFontFamilyFromData(data: ByteArray, descriptor: FontDescriptor): FontFamily =
    FontFamily(createFontFromData(data, descriptor))

/**
 * Creates a Compose Font object from loaded font data.
 *
 * This function wraps the raw font ByteArray in a platform-specific Font object
 * with the appropriate metadata (weight, style, identity).
 *
 * @param data The font data as ByteArray.
 * @param descriptor The parsed font descriptor with metadata.
 * @return A Compose Font object ready to be used in a FontFamily.
 */
internal expect fun createFontFromData(data: ByteArray, descriptor: FontDescriptor): Font
