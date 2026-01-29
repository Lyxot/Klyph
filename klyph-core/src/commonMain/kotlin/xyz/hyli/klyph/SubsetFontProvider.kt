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
import androidx.compose.ui.text.font.FontFamily

/**
 * Scope for [SubsetFontProvider] that provides access to scoped SubsetText function.
 *
 * This scope follows the same pattern as Compose's built-in scopes like RowScope and ColumnScope.
 * Within this scope, you can use SubsetText without specifying the provider parameter - it's
 * automatically provided from the scope.
 *
 * Example:
 * ```
 * SubsetFontProvider(provider = FontDescriptorProvider.fromCssUrl("https://example.com/fonts.css")) {
 *     // 'this' is SubsetFontScope
 *     SubsetText("Hello 世界") // No provider needed!
 *     SubsetText("More text") // Reuses the same provider
 * }
 * ```
 *
 * @property provider The FontDescriptorProvider used by scoped SubsetText calls.
 * @property fontFamily Optional fallback FontFamily for characters not covered by subset fonts.
 */
class SubsetFontScope internal constructor(
    internal val provider: FontDescriptorProvider,
    internal val fontFamily: FontFamily?
)

/**
 * Provides a FontDescriptorProvider context for font subsetting within the content scope.
 *
 * Within this scope, you can use [SubsetText] without specifying the provider parameter.
 * The provider is automatically provided from the scope.
 *
 * This is the recommended way to use Klyph for font subsetting, as it:
 * - Eliminates repetitive provider parameters
 * - Provides type-safe scoped API
 * - Follows familiar Compose patterns (similar to Row/Column)
 * - Enables all SubsetText calls within the scope to share the same provider
 *
 * Example:
 * ```
 * SubsetFontProvider(provider = FontDescriptorProvider.fromCssUrl("https://example.com/fonts.css")) {
 *     SubsetText(
 *         text = "你好世界 Hello World",
 *         fontSize = 20.sp
 *     )
 *     SubsetText(
 *         text = "Another text 另一段文字",
 *         fontSize = 16.sp,
 *         fontWeight = FontWeight.Bold
 *     )
 * }
 * ```
 *
 * @param provider The FontDescriptorProvider that supplies parsed font descriptors.
 * @param fontFamily Optional fallback FontFamily for characters not covered by subset fonts.
 * @param content The composable content within the SubsetFontScope.
 */
@Composable
fun SubsetFontProvider(
    provider: FontDescriptorProvider,
    fontFamily: FontFamily? = null,
    content: @Composable SubsetFontScope.() -> Unit
) {
    val scope = remember(provider, fontFamily) {
        SubsetFontScope(
            provider = provider,
            fontFamily = fontFamily
        )
    }
    scope.content()
}
