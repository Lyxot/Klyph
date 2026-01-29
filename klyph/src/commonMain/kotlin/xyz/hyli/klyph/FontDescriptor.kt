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
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import androidx.compose.ui.text.platform.Font as PlatformFont

/**
 * Font descriptor for loading fonts from remote URLs.
 *
 * This implementation fetches font data from a remote URL via HTTP,
 * making it suitable for web fonts and CDN-hosted fonts.
 *
 * @property url The URL of the font resource.
 * @property fontFamily The name of the font family.
 * @property weight The font weight (e.g., Normal, Bold, or custom weight).
 * @property style The font style (Normal or Italic).
 * @property unicodeRanges The list of Unicode ranges this font slice covers.
 */
data class UrlFontDescriptor(
    val url: String,
    override val fontFamily: String,
    override val weight: FontWeight,
    override val style: FontStyle,
    override val unicodeRanges: List<UnicodeRange>
) : FontDescriptor {
    override val cacheKey: String
        get() = "url:$url"

    override suspend fun getFontFamily(
        onBytesLoaded: (Long) -> Unit
    ): FontFamily {
        val res = httpClient.get(url)
        val fontData = res.bodyAsBytes()
        onBytesLoaded(res.contentLength() ?: fontData.size.toLong())
        return createFontFamilyFromData(fontData, this)
    }
}

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
    override val cacheKey: String
        get() = "hash:${resource.hashCode()}:${weight.hashCode()}:${style.hashCode()}:${unicodeRanges.hashCode()}"

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
 * (e.g., remote URLs via [UrlFontDescriptor], local resources via [ResourceFontDescriptor])
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
fun createFontFromData(data: ByteArray, descriptor: FontDescriptor): Font =
    PlatformFont(
        identity = "${descriptor.fontFamily}-${descriptor.weight.weight}-${descriptor.style}-${descriptor.hashCode()}",
        data = data,
        weight = descriptor.weight,
        style = descriptor.style
    )
