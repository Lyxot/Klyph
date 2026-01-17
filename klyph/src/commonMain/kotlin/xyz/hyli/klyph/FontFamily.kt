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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Scope for [SubsetFontProvider] that provides access to scoped SubsetText function.
 *
 * This scope follows the same pattern as Compose's built-in scopes like RowScope and ColumnScope.
 * Within this scope, you can use SubsetText without specifying the cssUrl parameter - it's
 * automatically provided from the scope.
 *
 * Example:
 * ```
 * SubsetFontProvider(cssUrl = "https://example.com/fonts.css") {
 *     // 'this' is SubsetFontScope
 *     SubsetText("Hello 世界") // No cssUrl needed!
 *     SubsetText("More text") // Reuses the same CSS URL
 * }
 * ```
 *
 * @property cssUrl The CSS URL provided by SubsetFontProvider, used by scoped SubsetText calls.
 */
class SubsetFontScope internal constructor(
    internal val cssUrl: String
)

/**
 * Provides a CSS URL context for font subsetting within the content scope.
 *
 * Within this scope, you can use [SubsetText] without specifying the cssUrl parameter.
 * The CSS URL is automatically provided from the scope.
 *
 * Example:
 * ```
 * SubsetFontProvider(cssUrl = "https://example.com/fonts.css") {
 *     SubsetText(
 *         text = "你好世界 Hello World",
 *         fontSize = 20.sp
 *     )
 * }
 * ```
 *
 * @param cssUrl The URL of the CSS file containing @font-face rules with unicode-range.
 * @param content The composable content within the SubsetFontScope.
 */
@Composable
fun SubsetFontProvider(
    cssUrl: String,
    content: @Composable SubsetFontScope.() -> Unit
) {
    val scope = remember(cssUrl) { SubsetFontScope(cssUrl) }
    scope.content()
}

/**
 * Fetches and parses CSS font descriptions from a URL.
 *
 * Automatically resolves relative URLs in the CSS against the CSS file's URL.
 * Results are cached globally to avoid redundant requests.
 *
 * @param url The URL of the CSS file.
 * @return A list of FontFace objects parsed from the CSS with resolved URLs.
 */
suspend fun getFontCssDescription(url: String): List<FontFace> {
    return CssCache.getOrLoad(url)
}

/**
 * Fetches binary font data from a URL.
 *
 * @param url The URL of the font file.
 * @return The font data as ByteArray.
 */
suspend fun getFontData(url: String): ByteArray {
    val res = httpClient.get(url)
    val fontData = res.body<ByteArray>()
    return fontData
}
