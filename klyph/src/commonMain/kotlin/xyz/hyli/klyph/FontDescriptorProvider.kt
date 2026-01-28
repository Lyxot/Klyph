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

/**
 * Interface for providing parsed font descriptors from various sources.
 *
 * Implementations can load descriptors from CSS URLs, CSS content strings,
 * or any other source.
 */
interface FontDescriptorProvider {
    /**
     * Loads and returns the list of parsed font descriptors.
     *
     * This function is called internally to fetch descriptors from the provider's source.
     *
     * @return List of parsed font descriptors.
     */
    suspend fun getDescriptors(): List<FontDescriptor>
}

/**
 * Implementation of FontDescriptorProvider that loads descriptors from a CSS URL.
 *
 * This provider fetches and parses the CSS file from the given URL,
 * using CssCache for caching and request deduplication.
 *
 * Example:
 * ```kotlin
 * val provider = CssUrlFontDescriptorProvider("https://example.com/fonts.css")
 * SubsetFontProvider(provider = provider) {
 *     SubsetText("Hello 世界")
 * }
 * ```
 *
 * @param cssUrl The URL of the CSS file containing @font-face rules.
 */
class CssUrlFontDescriptorProvider(
    private val cssUrl: String
) : FontDescriptorProvider {
    override suspend fun getDescriptors(): List<FontDescriptor> {
        return CssCache.getOrLoad(cssUrl)
    }
}

/**
 * Implementation of FontDescriptorProvider that parses descriptors from CSS content string.
 *
 * This provider parses the given CSS content string directly,
 * without fetching from a URL. Results are cached using CssCache to avoid re-parsing
 * the same content multiple times.
 *
 * The cache key is computed from a hash of the CSS content and base URL,
 * ensuring efficient lookups without storing the full content as a key.
 *
 * Example:
 * ```kotlin
 * val provider = CssContentFontDescriptorProvider("""
 *     @font-face {
 *         font-family: 'MyFont';
 *         src: url('font.woff2');
 *         unicode-range: U+0-FF;
 *     }
 * """)
 * SubsetFontProvider(provider = provider) {
 *     SubsetText("Hello")
 * }
 * ```
 *
 * @param cssContent The raw CSS content string containing @font-face rules.
 * @param baseUrl Optional base URL for resolving relative URLs in the CSS (defaults to empty string).
 */
class CssContentFontDescriptorProvider(
    private val cssContent: String,
    private val baseUrl: String = ""
) : FontDescriptorProvider {
    override suspend fun getDescriptors(): List<FontDescriptor> {
        return CssCache.getOrLoad(cssContent, baseUrl)
    }
}

/**
 * Implementation of FontDescriptorProvider that provides a static list of descriptors.
 *
 * This provider is useful for bundled fonts loaded from Compose resources
 * or when you have pre-constructed font descriptors that don't need fetching.
 *
 * Example:
 * ```kotlin
 * val descriptor = ResourceFontDescriptor(
 *     resource = Res.font.my_font,
 *     fontFamily = "MyFont",
 *     weight = FontWeight.Normal,
 *     style = FontStyle.Normal,
 *     unicodeRanges = listOf(UnicodeRange(0x0, 0xFF))
 * )
 * val provider = StaticFontDescriptorProvider(descriptor)
 * SubsetFontProvider(provider = provider) {
 *     SubsetText("Hello")
 * }
 * ```
 *
 * @param descriptors Variable number of font descriptors to provide.
 */
class StaticFontDescriptorProvider(
    vararg val descriptors: FontDescriptor
) : FontDescriptorProvider {
    override suspend fun getDescriptors(): List<FontDescriptor> {
        return descriptors.toList()
    }
}
