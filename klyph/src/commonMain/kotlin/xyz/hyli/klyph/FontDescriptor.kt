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

/**
 * Represents a parsed font descriptor with all metadata needed to load and apply a font slice.
 *
 * This class contains all the information needed to load and apply a specific font slice,
 * including the font URL, family name, weight, style, and Unicode ranges it covers.
 *
 * @property url The URL of the font resource.
 * @property fontFamily The name of the font family.
 * @property weight The font weight (e.g., Normal, Bold, or custom weight).
 * @property style The font style (Normal or Italic).
 * @property unicodeRanges The list of Unicode ranges this font slice covers.
 */
data class FontDescriptor(
    val url: String,
    val fontFamily: String,
    val weight: FontWeight,
    val style: FontStyle,
    val unicodeRanges: List<UnicodeRange>
)

/**
 * Creates a Compose Font instance from loaded font data.
 *
 * This function wraps the raw font ByteArray in a platform-specific Font object
 * with the appropriate metadata (weight, style, identity).
 *
 * @param data The font data as ByteArray.
 * @param descriptor The parsed font descriptor with metadata.
 * @return A Compose Font instance ready to be used in a FontFamily.
 */
fun createFontFromData(data: ByteArray, descriptor: FontDescriptor): Font {
    return PlatformFont(
        identity = "${descriptor.fontFamily}-${descriptor.weight.weight}-${descriptor.style}-${descriptor.url}",
        data = data,
        weight = descriptor.weight,
        style = descriptor.style
    )
}
