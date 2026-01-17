package xyz.hyli.klyph

import kotlin.text.Regex

/**
 * Represents a single font source entry from a @font-face rule's `src` descriptor.
 *
 * @param url The URL of the font resource, if specified.
 * @param local The name of a locally-installed font to use as a fallback, if specified.
 * @param format An optional format hint for the user agent (e.g., "woff2", "opentype").
 * @param techs A list of font technologies required by the font (e.g., "variations", "color-COLRv1").
 */
data class FontSrc(
    val url: String? = null,
    val local: String? = null,
    val format: String? = null,
    val techs: List<String> = emptyList()
)

/**
 * Represents a @font-face rule from CSS.
 *
 * Based on the CSS Fonts Module Level 4 specification.
 * https://drafts.csswg.org/css-fonts-4/
 */
data class FontFace(
    /**
     * The name of the font family.
     */
    val fontFamily: String,

    /**
     * A list of sources for the font, from which the user agent can select one.
     */
    val src: List<FontSrc>,

    /**
     * The style of the font (e.g., "normal", "italic", "oblique").
     */
    val fontStyle: String? = null,

    /**
     * The weight of the font (e.g., "normal", "bold", "400").
     */
    val fontWeight: String? = null,

    /**
     * The stretch of the font (e.g., "condensed", "expanded").
     */
    val fontStretch: String? = null,

    /**
     * How the font is displayed based on whether and when it is downloaded and ready to use.
     */
    val fontDisplay: String? = null,

    /**
     * The range of Unicode code points to be used from the font.
     */
    val unicodeRange: String? = null,

    /**
     * Specifies a variation of the font.
     */
    val fontVariant: String? = null,

    /**
     * Advanced control over OpenType font features.
     */
    val fontFeatureSettings: String? = null,

    /**
     * Low-level control over OpenType or TrueType font variations.
     */
    val fontVariationSettings: String? = null,

    /**
     * Overrides the ascent metric of the font.
     */
    val ascentOverride: String? = null,

    /**
     * Overrides the descent metric of the font.
     */
    val descentOverride: String? = null,

    /**
     * Overrides the line gap metric of the font.
     */
    val lineGapOverride: String? = null,

    /**
     * Overrides the language system of the font.
     */
    val fontLanguageOverride: String? = null,

    /**
     * Allows using named instances from variable fonts.
     */
    val fontNamedInstance: String? = null
)

/**
 * Parses a single src descriptor string (e.g., "url(...) format(...)") into a [FontSrc] object.
 * @param srcString The individual source string.
 * @return A [FontSrc] object, or null if neither a url() nor a local() is found.
 */
fun parseSrcString(srcString: String): FontSrc? {
    // Use non-greedy (.*?) to correctly handle content followed by other definitions like format().
    val urlRegex = """url\((.*?)\)""".toRegex()
    val localRegex = """local\((.*?)\)""".toRegex()
    val formatRegex = """format\((.*?)\)""".toRegex()
    val techRegex = """tech\((.*?)\)""".toRegex()

    val url = urlRegex.find(srcString)?.groupValues?.get(1)?.removeSurrounding("\"")?.removeSurrounding("'")
    val local = localRegex.find(srcString)?.groupValues?.get(1)?.removeSurrounding("\"")?.removeSurrounding("'")

    // A source must have at least a url or a local name.
    if (url == null && local == null) {
        return null
    }

    val format = formatRegex.find(srcString)?.groupValues?.get(1)?.removeSurrounding("\"")?.removeSurrounding("'")

    val techs = techRegex.find(srcString)?.groupValues?.get(1)?.split(Regex("""\s+"""))
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()

    return FontSrc(url = url, local = local, format = format, techs = techs)
}


/**
 * Parses a CSS string to extract @font-face rules and convert them into a list of FontFace objects.
 *
 * @param css The CSS content as a string.
 * @param baseUrl Optional base URL for resolving relative URLs in the CSS. If provided, all relative URLs in src descriptors will be resolved against this base URL.
 * @return A list of [FontFace] objects found in the CSS.
 */
fun parseCssToObjects(css: String, baseUrl: String? = null): List<FontFace> {
    // First, strip all /* ... */ comments from the entire CSS string
    val cssNoComments = css.replace(Regex("""(?s)/\*.*?\*/"""), "")

    val fontFaceRegex = """@font-face\s*\{([^{}]+)}""".toRegex()
    val fontFaceBlocks = fontFaceRegex.findAll(cssNoComments)
    val fontFaces = mutableListOf<FontFace>()

    for (block in fontFaceBlocks) {
        val content = block.groupValues[1].trim()

        // Split descriptors by semicolon
        val descriptors = content.split(';').map { it.trim() }.filter { it.isNotEmpty() }

        // Create a list of all key-value pairs to handle multiple 'src' descriptors
        val descriptorPairs = descriptors.mapNotNull {
            val parts = it.split(':', limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
        }

        // font-family is required, take the first one found
        val fontFamily = descriptorPairs.find { it.first == "font-family" }
            ?.second
            ?.removeSurrounding("\"")?.removeSurrounding("'")
            ?: continue

        // Find all 'src' values
        val allSrcValues = descriptorPairs
            .filter { it.first == "src" }
            .map { it.second.removeSurrounding("\"").removeSurrounding("'") }

        // Process all src values, splitting each by comma if needed
        val srcStringList = allSrcValues.flatMap { srcValue ->
            // Split by comma, but only if it's followed by url( or local( to avoid splitting data URIs
            srcValue.split(Regex(""",\s*(?=url\(|local\()"""))
                .map { it.trim() }
        }.filter { it.isNotEmpty() }

        if (srcStringList.isEmpty()) {
            continue
        }

        // Parse each source string into a structured FontSrc object
        val srcList = srcStringList.mapNotNull { parseSrcString(it) }

        if (srcList.isEmpty()) {
            continue
        }

        // Resolve relative URLs if baseUrl is provided
        val resolvedSrcList = if (baseUrl != null) {
            srcList.map { src ->
                src.copy(url = src.url?.let { resolveUrl(baseUrl, it) })
            }
        } else {
            srcList
        }

        // For single-value descriptors, a simple map is fine (last one wins if duplicated)
        val singleValueDescriptorMap = descriptorPairs.toMap()

        fun getDescriptorValue(name: String): String? {
            return singleValueDescriptorMap[name]?.removeSurrounding("\"")?.removeSurrounding("'")
        }

        val fontFace = FontFace(
            fontFamily = fontFamily,
            src = resolvedSrcList,
            fontStyle = getDescriptorValue("font-style"),
            fontWeight = getDescriptorValue("font-weight"),
            fontStretch = getDescriptorValue("font-stretch"),
            fontDisplay = getDescriptorValue("font-display"),
            unicodeRange = getDescriptorValue("unicode-range"),
            fontVariant = getDescriptorValue("font-variant"),
            fontFeatureSettings = getDescriptorValue("font-feature-settings"),
            fontVariationSettings = getDescriptorValue("font-variation-settings"),
            ascentOverride = getDescriptorValue("ascent-override"),
            descentOverride = getDescriptorValue("descent-override"),
            lineGapOverride = getDescriptorValue("line-gap-override"),
            fontLanguageOverride = getDescriptorValue("font-language-override"),
            fontNamedInstance = getDescriptorValue("font-named-instance")
        )
        fontFaces.add(fontFace)
    }
    return fontFaces
}