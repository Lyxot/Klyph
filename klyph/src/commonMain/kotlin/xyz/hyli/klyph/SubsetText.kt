package xyz.hyli.klyph

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit

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
 * @param fontFamily Optional FontFamily override. If provided, disables font subsetting.
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
 * @param fontFamily Optional FontFamily override. If provided, disables font subsetting.
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
    // If manual fontFamily override, use standard Text
    if (fontFamily != null) {
        Text(
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
            style = style
        )
        return
    }

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
 * 2. Analyzes each character to determine which font slice it needs
 * 3. Loads the necessary font slices
 * 4. Builds an AnnotatedString where each character span gets its own FontFamily
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
): androidx.compose.ui.text.AnnotatedString {
    // Parse CSS and cache the font faces
    val fontFaces by produceState<List<FontFace>>(emptyList(), cssUrl) {
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

    // Map each character to its font descriptor
    val charToDescriptor = remember(text, descriptors) {
        buildCharToDescriptorMap(text, descriptors)
    }

    // Load fonts for each unique descriptor
    var descriptorToFontFamily by remember {
        mutableStateOf<Map<ParsedFontDescriptor, FontFamily>>(emptyMap())
    }

    LaunchedEffect(charToDescriptor) {
        if (charToDescriptor.isEmpty()) {
            descriptorToFontFamily = emptyMap()
            return@LaunchedEffect
        }

        val uniqueDescriptors = charToDescriptor.values.toSet()
        val newMap = mutableMapOf<ParsedFontDescriptor, FontFamily>()

        for (descriptor in uniqueDescriptors) {
            try {
                val fontData = FontSliceCache.getOrLoad(descriptor.url)
                val font = createFontFromData(fontData, descriptor)
                // Each FontFamily contains ONLY ONE font slice
                newMap[descriptor] = FontFamily(font)
            } catch (e: Exception) {
                println("ERROR: Failed to load font from ${descriptor.url}: ${e.message}")
            }
        }

        descriptorToFontFamily = newMap
    }

    // Build the annotated string
    return remember(text, charToDescriptor, descriptorToFontFamily) {
        buildAnnotatedString {
            if (descriptorToFontFamily.isEmpty()) {
                // No fonts loaded yet, just append plain text
                append(text)
                return@buildAnnotatedString
            }

            var currentIndex = 0
            while (currentIndex < text.length) {
                val char = text[currentIndex]
                val descriptor = charToDescriptor[char]
                val fontFamily = descriptor?.let { descriptorToFontFamily[it] }

                if (fontFamily != null) {
                    // Find consecutive characters with the same font
                    var endIndex = currentIndex + 1
                    while (endIndex < text.length && charToDescriptor[text[endIndex]] == descriptor) {
                        endIndex++
                    }

                    // Apply font to this span
                    withStyle(SpanStyle(fontFamily = fontFamily)) {
                        append(text.substring(currentIndex, endIndex))
                    }

                    currentIndex = endIndex
                } else {
                    // No font for this character, use default
                    // Find consecutive characters without fonts
                    var endIndex = currentIndex + 1
                    while (endIndex < text.length && charToDescriptor[text[endIndex]] == null) {
                        endIndex++
                    }

                    withStyle(SpanStyle(fontFamily = FontFamily.Default)) {
                        append(text.substring(currentIndex, endIndex))
                    }

                    currentIndex = endIndex
                }
            }
        }
    }
}

/**
 * Builds a map from characters to their font descriptors based on unicode-range matching.
 *
 * @param text The text to analyze.
 * @param descriptors The list of available font descriptors.
 * @return A map from Char to ParsedFontDescriptor.
 */
private fun buildCharToDescriptorMap(
    text: String,
    descriptors: List<ParsedFontDescriptor>
): Map<Char, ParsedFontDescriptor> {
    if (text.isEmpty() || descriptors.isEmpty()) {
        return emptyMap()
    }

    val map = mutableMapOf<Char, ParsedFontDescriptor>()
    val uniqueChars = text.toSet()

    for (char in uniqueChars) {
        // Find the first descriptor whose unicode-range includes this character
        val descriptor = descriptors.firstOrNull { descriptor ->
            // If no unicode-range, it covers all characters
            descriptor.unicodeRanges.isEmpty() || isCharInRanges(char, descriptor.unicodeRanges)
        }

        if (descriptor != null) {
            map[char] = descriptor
        }
    }

    return map
}
