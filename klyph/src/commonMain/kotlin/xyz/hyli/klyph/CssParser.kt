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

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Parses a CSS string to extract @font-face rules and convert them into ParsedFontDescriptor objects.
 *
 * This function directly creates ParsedFontDescriptor objects from CSS, skipping intermediate representations.
 * It automatically resolves relative URLs against the base URL if provided.
 *
 * @param css The CSS content as a string.
 * @param baseUrl Optional base URL for resolving relative URLs in the CSS. If provided, all relative URLs in src descriptors will be resolved against this base URL.
 * @return A list of [ParsedFontDescriptor] objects found in the CSS.
 */
fun parseCssToDescriptors(css: String, baseUrl: String? = null): List<ParsedFontDescriptor> {
    // Strip all /* ... */ comments from the entire CSS string
    val cssNoComments = css.replace(Regex("""(?s)/\*.*?\*/"""), "")

    val fontFaceRegex = """@font-face\s*\{([^{}]+)}""".toRegex()
    val fontFaceBlocks = fontFaceRegex.findAll(cssNoComments)
    val descriptors = mutableListOf<ParsedFontDescriptor>()

    for (block in fontFaceBlocks) {
        val content = block.groupValues[1].trim()

        // Split descriptors by semicolon
        val cssDescriptors = content.split(';').map { it.trim() }.filter { it.isNotEmpty() }

        // Create a map of descriptor key-value pairs
        val descriptorMap = cssDescriptors.mapNotNull {
            val parts = it.split(':', limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
        }.toMap()

        fun getValue(name: String): String? {
            return descriptorMap[name]?.removeSurrounding("\"")?.removeSurrounding("'")
        }

        // font-family is required
        val fontFamily = getValue("font-family") ?: continue

        // Extract font URL from src descriptor
        val srcValue = getValue("src") ?: continue
        val url = extractUrlFromSrc(srcValue) ?: continue

        // Resolve relative URL if base URL provided
        val resolvedUrl = if (baseUrl != null) {
            resolveUrl(baseUrl, url)
        } else {
            url
        }

        // Parse font weight
        val weight = parseFontWeight(getValue("font-weight"))

        // Parse font style
        val style = parseFontStyle(getValue("font-style"))

        // Parse unicode ranges
        val unicodeRanges = parseUnicodeRange(getValue("unicode-range"))

        descriptors.add(
            ParsedFontDescriptor(
                url = resolvedUrl,
                fontFamily = fontFamily,
                weight = weight,
                style = style,
                unicodeRanges = unicodeRanges
            )
        )
    }

    return descriptors
}

/**
 * Extracts the first URL from a CSS src descriptor.
 *
 * Supports multiple formats:
 * - url("path/to/font.woff2")
 * - url('path/to/font.woff2')
 * - url(path/to/font.woff2)
 * - Multiple sources: url(...), url(...) (returns first URL)
 *
 * Local fonts (local(...)) are ignored for web compatibility.
 *
 * @param srcValue The src descriptor value from CSS.
 * @return The first URL found, or null if no URL is present.
 */
private fun extractUrlFromSrc(srcValue: String): String? {
    // Match url(...) pattern, using non-greedy match
    val urlRegex = """url\((.*?)\)""".toRegex()
    val match = urlRegex.find(srcValue) ?: return null

    // Extract URL and remove quotes if present
    return match.groupValues[1]
        .removeSurrounding("\"")
        .removeSurrounding("'")
        .trim()
}

/**
 * Parses a CSS font-weight value into a FontWeight.
 *
 * Supports named values ("normal", "bold") and numeric values (100-900).
 *
 * @param weightStr The font-weight value from CSS (e.g., "normal", "bold", "400").
 * @return The corresponding FontWeight, defaulting to Normal if parsing fails.
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
 * Supports "normal", "italic", and "oblique" (treated as italic).
 *
 * @param styleStr The font-style value from CSS (e.g., "normal", "italic").
 * @return The corresponding FontStyle, defaulting to Normal if parsing fails.
 */
private fun parseFontStyle(styleStr: String?): FontStyle {
    return when (styleStr?.lowercase()) {
        "italic", "oblique" -> FontStyle.Italic
        else -> FontStyle.Normal
    }
}
