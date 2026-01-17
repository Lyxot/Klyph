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

package sample.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.hyli.klyph.*
import kotlin.js.ExperimentalWasmJsInterop

// Example CSS URL with font subsetting (uses unicode-range)
// This is a Google Fonts style CSS that serves font slices based on unicode ranges
// Note: Klyph automatically resolves relative URLs in the CSS file (e.g., url(./font.woff2))
const val cssUrl = "https://unpkg.com/misans@4.1.0/lib/Normal/MiSans-Regular.min.css"

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
fun App() {
    SubsetFontProvider(cssUrl = cssUrl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Klyph Font Subsetting Demo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Demo 1: Basic font subsetting
            Text(
                text = "Demo 1: Basic Font Subsetting",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Chinese text - will only load font slices for these specific characters
                // Note: No cssUrl parameter needed - it's provided by the SubsetFontProvider scope!
                SubsetText(
                    text = "Chinese: 你好世界！这是中文字体",
                    fontSize = 16.sp
                )

                // Mixed text - loads slices for both Chinese and Latin characters
                SubsetText(
                    text = "Mixed: Hello 世界! Klyph is awesome!",
                    fontSize = 16.sp
                )

                // Numbers and punctuation
                SubsetText(
                    text = "Numbers: 0123456789 ,.!?;:()[]",
                    fontSize = 16.sp
                )

                // Bold text example
                SubsetText(
                    text = "粗体文字 Bold Chinese",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            // Demo 2: More examples
            Text(
                text = "Demo 2: Mixed Language Text",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SubsetText(
                    text = "完美渲染中英文混排 Perfect mixed text rendering!",
                    fontSize = 16.sp
                )

                SubsetText(
                    text = "支持多种样式 Supports multiple styles",
                    fontSize = 16.sp,
                )

                SubsetText(
                    text = "每个字符都能正确显示 Every character renders correctly 123",
                    fontSize = 14.sp
                )
            }

            HorizontalDivider()

            // Demo 3: Show loaded font faces from CSS
            Text(
                text = "Demo 3: Font Faces from CSS",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            var fontFaces by remember { mutableStateOf<List<FontFace>>(emptyList()) }
            var cacheSize by remember { mutableStateOf(0) }

            LaunchedEffect(Unit) {
                try {
                    fontFaces = getFontCssDescription(cssUrl)
                    cacheSize = FontSliceCache.size()
                } catch (e: Exception) {
                    println("Error fetching font CSS description: ${e.message}")
                }
            }

            Text(
                text = "Loaded ${fontFaces.size} font faces from CSS",
                fontSize = 14.sp
            )
            Text(
                text = "Cache contains $cacheSize font slices",
                fontSize = 14.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                fontFaces.take(5).forEach { fontFace ->
                    Text(
                        text = "• ${fontFace.fontFamily} | Weight: ${fontFace.fontWeight ?: "default"} | Unicode: ${fontFace.unicodeRange?.take(50) ?: "all"}",
                        fontSize = 12.sp
                    )
                }
                if (fontFaces.size > 5) {
                    Text(
                        text = "... and ${fontFaces.size - 5} more",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

