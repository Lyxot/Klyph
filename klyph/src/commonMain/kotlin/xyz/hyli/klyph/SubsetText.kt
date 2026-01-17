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

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.launch

/**
 * A Text composable within [SubsetFontProvider] scope that automatically loads font slices
 * and applies them character-by-character.
 *
 * This is the recommended way to use Klyph for font subsetting. It's fully compatible with
 * the standard Material3 Text API and automatically uses the CSS URL from the
 * [SubsetFontProvider] scope.
 *
 * Internally builds an AnnotatedString where each character gets the appropriate font slice
 * based on unicode-range matching. Unlike combining all fonts into one FontFamily (which
 * doesn't work in Compose), this renders each character with its own FontFamily containing
 * only the necessary font slice.
 *
 * Example:
 * ```
 * SubsetFontProvider(cssUrl = "https://example.com/fonts.css") {
 *     SubsetText(
 *         text = "Hello 世界!",
 *         fontSize = 20.sp,
 *         fontWeight = FontWeight.Bold
 *     )
 * }
 * ```
 *
 * @param text The text to display.
 * @param modifier Modifier to be applied to the text.
 * @param color The color of the text.
 * @param fontSize The font size of the text.
 * @param fontStyle The font style (normal, italic).
 * @param fontWeight The font weight (normal, bold, etc.).
 * @param fontFamily Optional FontFamily to use as a fallback for characters not covered by subset fonts.
 * @param letterSpacing The spacing between characters.
 * @param textDecoration The decoration to apply (underline, strikethrough).
 * @param textAlign The alignment of the text.
 * @param lineHeight The height of each line.
 * @param overflow How to handle text overflow.
 * @param softWrap Whether the text should break at soft line breaks.
 * @param maxLines The maximum number of lines.
 * @param minLines The minimum number of lines.
 * @param onTextLayout Callback for text layout result.
 * @param style The text style to apply.
 */
@Composable
fun SubsetFontScope.SubsetText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = this.fontFamily,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) = SubsetText(
    text = text,
    modifier = modifier,
    color = color,
    fontSize = fontSize,
    fontStyle = fontStyle,
    fontWeight = fontWeight,
    fontFamily = fontFamily,
    letterSpacing = letterSpacing,
    textDecoration = textDecoration,
    textAlign = textAlign,
    lineHeight = lineHeight,
    overflow = overflow,
    softWrap = softWrap,
    maxLines = maxLines,
    minLines = minLines,
    onTextLayout = onTextLayout,
    style = style,
    cssUrl = cssUrl
)

/**
 * A Text composable that automatically loads font slices and applies them character-by-character.
 *
 * This is a standalone version that requires an explicit CSS URL. For better ergonomics,
 * prefer using the scoped version within [SubsetFontProvider].
 *
 * This composable is fully compatible with the standard Material3 Text API. It internally
 * builds an AnnotatedString where each character gets the appropriate font slice based on
 * unicode-range matching.
 *
 * Unlike the standard approach of combining all fonts into one FontFamily (which doesn't work
 * in Compose), this renders each character with its own FontFamily containing only the
 * necessary font slice.
 *
 * Example:
 * ```
 * SubsetText(
 *     text = "Hello 世界!",
 *     cssUrl = "https://example.com/fonts.css",
 *     fontSize = 20.sp
 * )
 * ```
 *
 * @param text The text to display.
 * @param modifier Modifier to be applied to the text.
 * @param color The color of the text.
 * @param fontSize The font size of the text.
 * @param fontStyle The font style (normal, italic).
 * @param fontWeight The font weight (normal, bold, etc.).
 * @param fontFamily Optional FontFamily to use as a fallback for characters not covered by subset fonts.
 * @param letterSpacing The spacing between characters.
 * @param textDecoration The decoration to apply (underline, strikethrough).
 * @param textAlign The alignment of the text.
 * @param lineHeight The height of each line.
 * @param overflow How to handle text overflow.
 * @param softWrap Whether the text should break at soft line breaks.
 * @param maxLines The maximum number of lines.
 * @param minLines The minimum number of lines.
 * @param onTextLayout Callback for text layout result.
 * @param style The text style to apply.
 * @param cssUrl The URL of the CSS file containing @font-face rules with unicode-range.
 */
@Composable
fun SubsetText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    cssUrl: String
) {
    // Build annotated string with per-character font slices
    val annotatedString = rememberSubsetAnnotatedString(
        cssUrl = cssUrl,
        text = text,
        requestedWeight = fontWeight,
        requestedStyle = fontStyle
    )

    Text(
        text = annotatedString,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

/**
 * Remembers and builds an AnnotatedString with character-level font slice assignments.
 *
 * This function:
 * 1. Fetches and parses the CSS to get font face definitions
 * 2. Analyzes the text to produce font intervals (runs of text using the same font)
 * 3. Loads the necessary font slices
 * 4. Builds an AnnotatedString where each interval gets its FontFamily
 *
 * @param cssUrl The URL of the CSS file containing @font-face rules.
 * @param text The text to render.
 * @param requestedWeight The desired font weight.
 * @param requestedStyle The desired font style.
 * @return An AnnotatedString with proper font assignments.
 */
@Composable
private fun rememberSubsetAnnotatedString(
    cssUrl: String,
    text: String,
    requestedWeight: FontWeight?,
    requestedStyle: FontStyle?
): AnnotatedString {
    // Parse CSS and cache the font faces
    val fontFaces by produceState(emptyList(), cssUrl) {
        try {
            value = getFontCssDescription(cssUrl)
        } catch (e: Exception) {
            println("ERROR: Failed to load CSS from $cssUrl: ${e.message}")
            value = emptyList()
        }
    }

    // Parse font descriptors
    val descriptors = remember(fontFaces, requestedWeight, requestedStyle) {
        fontFaces.mapNotNull { parseFontDescriptor(it) }
            .filter { descriptor ->
                val weightMatches = requestedWeight == null || descriptor.weight == requestedWeight
                val styleMatches = requestedStyle == null || descriptor.style == requestedStyle
                weightMatches && styleMatches
            }
    }

    // Analyze text to produce intervals - Amortized O(N) time due to locality hint, O(I) space
    val textIntervals = remember(text, descriptors) {
        if (text.isEmpty() || descriptors.isEmpty()) {
            emptyList()
        } else {
            buildList {
                var startIndex = 0
                var currentDescriptor = findDescriptor(text[0], descriptors)

                for (i in 1 until text.length) {
                    val nextDescriptor = findDescriptor(text[i], descriptors, currentDescriptor)
                    if (nextDescriptor != currentDescriptor) {
                        add(TextInterval(startIndex, i, currentDescriptor))
                        startIndex = i
                        currentDescriptor = nextDescriptor
                    }
                }
                add(TextInterval(startIndex, text.length, currentDescriptor))
            }
        }
    }

    // Load fonts for each unique descriptor found in intervals
    var descriptorToFontFamily by remember {
        mutableStateOf<Map<ParsedFontDescriptor, FontFamily>>(emptyMap())
    }

    LaunchedEffect(textIntervals) {
        val uniqueDescriptors = textIntervals.mapNotNull { it.descriptor }.toSet()
        val missingDescriptors = uniqueDescriptors.filter { it !in descriptorToFontFamily }

        missingDescriptors.forEach { descriptor ->
            launch {
                try {
                    val fontData = FontSliceCache.getOrLoad(descriptor.url)
                    val font = createFontFromData(fontData, descriptor)
                    descriptorToFontFamily += (descriptor to FontFamily(font))
                } catch (e: Exception) {
                    println("ERROR: Failed to load font from ${descriptor.url}: ${e.message}")
                }
            }
        }
    }

    // Build the annotated string - O(I) time
    return remember(text, textIntervals, descriptorToFontFamily) {
        buildAnnotatedString {
            if (textIntervals.isEmpty() || descriptorToFontFamily.isEmpty()) {
                append(text)
                return@buildAnnotatedString
            }

            for (interval in textIntervals) {
                val substring = text.substring(interval.start, interval.end)
                val fontFamily = interval.descriptor?.let { descriptorToFontFamily[it] }

                if (fontFamily != null) {
                    withStyle(SpanStyle(fontFamily = fontFamily)) {
                        append(substring)
                    }
                } else {
                    append(substring)
                }
            }
        }
    }
}

/**
 * Represents a contiguous run of text that uses the same font descriptor.
 */
private data class TextInterval(
    val start: Int,
    val end: Int,
    val descriptor: ParsedFontDescriptor?
)

/**
 * Finds the first descriptor whose unicode-range includes this character.
 *
 * Uses an optional [hint] (typically the last used descriptor) to optimize lookup time
 * for sequential characters in the same script range from O(D) to O(1).
 */
private fun findDescriptor(
    char: Char,
    descriptors: List<ParsedFontDescriptor>,
    hint: ParsedFontDescriptor? = null
): ParsedFontDescriptor? {
    if (hint != null && (hint.unicodeRanges.isEmpty() || isCharInRanges(char, hint.unicodeRanges))) {
        return hint
    }
    return descriptors.firstOrNull { descriptor ->
        descriptor.unicodeRanges.isEmpty() || isCharInRanges(char, descriptor.unicodeRanges)
    }
}
