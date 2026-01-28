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

import androidx.compose.runtime.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.launch

/**
 * Scoped version of [rememberSubsetAnnotatedString] that automatically fetches font descriptors
 * from the provider in [SubsetFontProvider] scope.
 *
 * This is a low-level composable for direct AnnotatedString access within a [SubsetFontProvider].
 * Use this when you need more control than [SubsetText] provides, such as:
 * - Building custom composables that use subset fonts
 * - Combining multiple AnnotatedStrings
 * - Using the result in non-Text contexts
 * - Applying additional styling to the AnnotatedString
 *
 * Example:
 * ```kotlin
 * SubsetFontProvider(provider = FontDescriptorProvider.fromCssUrl("https://example.com/fonts.css")) {
 *     val annotatedString = rememberSubsetAnnotatedString(
 *         text = "Hello 世界",
 *         requestedWeight = FontWeight.Normal,
 *         requestedStyle = FontStyle.Normal
 *     )
 *
 *     // Use the AnnotatedString however you like
 *     Text(
 *         text = annotatedString,
 *         fontFamily = myFallbackFont,
 *         modifier = Modifier.clickable { /* custom behavior */ }
 *     )
 * }
 * ```
 *
 * @param text The text to render.
 * @param requestedWeight The desired font weight, or null to match any weight.
 * @param requestedStyle The desired font style, or null to match any style.
 * @return An AnnotatedString with font slices applied to matching character ranges.
 */
@Composable
fun SubsetFontScope.rememberSubsetAnnotatedString(
    text: String,
    requestedWeight: FontWeight? = null,
    requestedStyle: FontStyle? = null
): AnnotatedString {
    // Fetch and cache the font descriptors from provider
    val allDescriptors by produceState(emptyList(), provider) {
        try {
            value = provider.getDescriptors()
        } catch (e: Exception) {
            println("ERROR: Failed to load descriptors from provider: ${e.message}")
            value = emptyList()
        }
    }

    return rememberSubsetAnnotatedString(
        descriptors = allDescriptors,
        text = text,
        requestedWeight = requestedWeight,
        requestedStyle = requestedStyle
    )
}

/**
 * Remembers and builds an AnnotatedString with character-level font slice assignments.
 *
 * This is a low-level composable that provides direct access to the AnnotatedString
 * building process. Use this when you need more control than [SubsetText] provides,
 * such as:
 * - Building custom composables that use subset fonts
 * - Combining multiple AnnotatedStrings
 * - Using the result in non-Text contexts
 * - Applying additional styling to the AnnotatedString
 *
 * The function:
 * 1. Filters descriptors by requested weight and style
 * 2. Analyzes the text to produce font intervals (runs of text using the same font)
 * 3. Loads the necessary font slices on-demand
 * 4. Builds an AnnotatedString where each interval gets its FontFamily
 *
 * Characters without matching font slices will be rendered without any font assignment
 * (using the system default or fallback font you provide when rendering).
 *
 * Example:
 * ```kotlin
 * val descriptors by produceState(emptyList(), cssUrl) {
 *     value = CssCache.getOrLoad(cssUrl)
 * }
 * val annotatedString = rememberSubsetAnnotatedString(
 *     descriptors = descriptors,
 *     text = "Hello 世界",
 *     requestedWeight = FontWeight.Normal,
 *     requestedStyle = FontStyle.Normal
 * )
 * Text(
 *     text = annotatedString,
 *     fontFamily = myFallbackFont // Used for unmapped characters
 * )
 * ```
 *
 * @param descriptors The list of parsed font descriptors to use.
 * @param text The text to render.
 * @param requestedWeight The desired font weight, or null to match any weight.
 * @param requestedStyle The desired font style, or null to match any style.
 * @return An AnnotatedString with font slices applied to matching character ranges.
 */
@Composable
fun rememberSubsetAnnotatedString(
    descriptors: List<FontDescriptor>,
    text: String,
    requestedWeight: FontWeight? = null,
    requestedStyle: FontStyle? = null
): AnnotatedString {
    // Filter descriptors by requested weight and style
    val filteredDescriptors = remember(descriptors, requestedWeight, requestedStyle) {
        descriptors.filter { descriptor ->
            isWeightMatching(requestedWeight, descriptor.weight) &&
                    isStyleMatching(requestedStyle, descriptor.style)
        }.let {
            if (it.isEmpty() && requestedStyle == FontStyle.Italic) {
                // Fallback: ignore style if no matching italic fonts found
                descriptors.filter { descriptor ->
                    isWeightMatching(requestedWeight, descriptor.weight)
                }
            } else {
                it
            }
        }
    }

    // Analyze text to produce intervals - Amortized O(N) time due to locality hint, O(I) space
    val textIntervals = remember(text, filteredDescriptors) {
        if (text.isEmpty() || filteredDescriptors.isEmpty()) {
            emptyList()
        } else {
            buildList {
                var startIndex = 0
                var currentDescriptor = findDescriptor(text[0], filteredDescriptors)

                for (i in 1 until text.length) {
                    val nextDescriptor = findDescriptor(text[i], filteredDescriptors, currentDescriptor)
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
        mutableStateOf<Map<FontDescriptor, FontFamily>>(emptyMap())
    }

    LaunchedEffect(textIntervals) {
        val uniqueDescriptors = textIntervals.mapNotNull { it.descriptor }.toSet()
        val missingDescriptors = uniqueDescriptors.filter { it !in descriptorToFontFamily }

        missingDescriptors.forEach { descriptor ->
            launch {
                try {
                    val font = FontSliceCache.getOrLoad(descriptor)
                    descriptorToFontFamily += (descriptor to FontFamily(font))
                } catch (e: Exception) {
                    println("ERROR: Failed to load font from ${descriptor.cacheKey}: ${e.message}")
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
    val descriptor: FontDescriptor?
)

/**
 * Finds the first descriptor whose unicode-range includes this character.
 *
 * Uses an optional [hint] (typically the last used descriptor) to optimize lookup time
 * for sequential characters in the same script range from O(D) to O(1).
 */
private fun findDescriptor(
    char: Char,
    descriptors: List<FontDescriptor>,
    hint: FontDescriptor? = null
): FontDescriptor? {
    if (hint != null && (hint.unicodeRanges.isEmpty() || isCharInRanges(char, hint.unicodeRanges))) {
        return hint
    }
    return descriptors.firstOrNull { descriptor ->
        descriptor.unicodeRanges.isEmpty() || isCharInRanges(char, descriptor.unicodeRanges)
    }
}

/**
 * Checks if the requested font weight matches the actual font weight.
 *
 * If [requested] is null, it matches any [actual] weight.
 */
private fun isWeightMatching(requested: FontWeight?, actual: FontWeight): Boolean =
    requested == null || requested == actual

/**
 * Checks if the requested font style matches the actual font style.
 *
 * If [requested] is null, it matches any [actual] style.
 */
private fun isStyleMatching(requested: FontStyle?, actual: FontStyle): Boolean =
    requested == null || requested == actual
