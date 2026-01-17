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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
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

            Text(
                text = "Font Faces from CSS",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            var fontFaces by remember { mutableStateOf<List<FontFace>>(emptyList()) }
            val fontCacheSize by FontSliceCache.size.collectAsState()
            val cssCacheSize by CssCache.size.collectAsState()

            LaunchedEffect(Unit) {
                try {
                    fontFaces = getFontCssDescription(cssUrl)
                } catch (e: Exception) {
                    println("Error fetching font CSS description: ${e.message}")
                }
            }

            Text(
                text = "Loaded ${fontFaces.size} font faces from CSS",
                fontSize = 14.sp
            )
            Text(
                text = "Font cache: $fontCacheSize slices | CSS cache: $cssCacheSize files",
                fontSize = 14.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                fontFaces.take(3).forEach { fontFace ->
                    Text(
                        text = "• ${fontFace.fontFamily} | Weight: ${fontFace.fontWeight ?: "default"} | Unicode: ${
                            fontFace.unicodeRange?.take(
                                80
                            ) ?: "all"
                        }",
                        fontSize = 12.sp
                    )
                }
                if (fontFaces.size > 3) {
                    Text(
                        text = "... and ${fontFaces.size - 3} more",
                        fontSize = 12.sp
                    )
                }
            }

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

            // Demo 3: Large Text Content
            Text(
                text = "Demo 3: Large Text Content",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SubsetText(
                    text = """
                        Klyph 字体子集化库

                        Klyph 是一个创新的字体管理解决方案，专为 Compose Multiplatform 应用程序设计。
                        它采用了网页开发中常用的字体切片技术，通过智能分析文本内容，仅加载所需的字符字形，
                        从而显著减少网络传输数据量。这种方法特别适合处理包含大量字符的中日韩（CJK）字体文件。

                        Intelligent Font Subsetting for Modern Applications

                        Klyph brings web-style font subsetting to Compose Multiplatform, dramatically
                        reducing font loading overhead by fetching only the specific character slices
                        needed for your text. This is particularly valuable for applications using
                        large font files like Chinese, Japanese, or Korean typefaces, which can easily
                        exceed 10-15 MB per weight.

                        核心技术特点：
                        • 基于 CSS @font-face 规则的智能解析
                        • Unicode 范围匹配算法
                        • 全局缓存与请求去重
                        • 按字符级别应用字体
                        • 零配置，开箱即用

                        Key Technical Features:
                        • CSS @font-face rule parsing
                        • Unicode range matching algorithm
                        • Global caching with request deduplication
                        • Character-level font application
                        • Zero configuration, works out of the box

                        性能优势：通过只加载实际使用的字符，Klyph 可以将字体数据传输量减少 99% 以上。
                        例如，一个 12 MB 的中文字体，如果只渲染 100 个不同的汉字，实际下载量可能只有 50-100 KB。

                        Performance Benefits: By loading only the characters actually used, Klyph can
                        reduce font data transfer by over 99%. For example, a 12 MB Chinese font file,
                        when rendering only 100 different characters, might result in just 50-100 KB
                        of actual downloads.
                    """.trimIndent(),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Text(
                    text = "This large text demonstrates efficient loading of mixed CJK and Latin characters. " +
                            "Notice how only the specific font slices needed for these characters are loaded, " +
                            "not the entire multi-megabyte font file.",
                    fontSize = 12.sp
                )
            }
        }
    }
}

