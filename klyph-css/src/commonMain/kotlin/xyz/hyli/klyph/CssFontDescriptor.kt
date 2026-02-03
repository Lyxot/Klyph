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

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

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
    override val unicodeRanges: UnicodeRangeList
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
