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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sample.app.fonts.MiSansBoldBaseUrl
import sample.app.fonts.MiSansBoldCssContent
import sample.app.fonts.MiSansRegular
import sample.app.fonts.MoonStarsKaiTRegular
import xyz.hyli.klyph.*

@Composable
fun App() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(0.5f)
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier)
            // Title
            Text(
                text = "Klyph Font Subsetting Demo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            SubsetFontProvider(provider = CssUrlFontDescriptorProvider(MiSansRegular)) {
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
                        text = "中英文混排 Mixed text rendering!",
                        fontSize = 16.sp
                    )

                    SubsetText(
                        text = "支持多种样式 Supports multiple styles",
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
                    )

                    // Bold text example
                    SubsetText(
                        text = "粗体文字 Bold Chinese",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        provider = CssContentFontDescriptorProvider(MiSansBoldCssContent, MiSansBoldBaseUrl)
                    )

                    SubsetText(
                        text = "字符匹配字体切片 Characters matched to font slices 123",
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
                        Klyph

                        传统的 Compose 字体加载方式存在明显的性能瓶颈。当应用需要渲染中文、日文或韩文等东亚文字时，往往需要下载完整的字体文件。这些字体文件通常包含数万个字符，体积可达 10-15 MB，即使应用只需要显示其中的几十个汉字。这种做法不仅浪费带宽，还会显著延长页面加载时间，影响用户体验。Klyph 通过引入智能字体子集化技术，有效解决了这一问题。

                        The Traditional Font Loading Problem

                        In traditional web and mobile applications, rendering CJK (Chinese, Japanese, Korean) text requires downloading entire font files containing tens of thousands of glyphs. A typical Chinese font file weighs 10-15 MB per weight, yet most applications only display a few hundred unique characters. This approach wastes bandwidth, slows down page loads, and degrades user experience, especially on mobile networks.

                        Klyph 的技术创新

                        Klyph 采用了与现代网页开发相同的字体切片技术。它会解析 CSS 文件中的 @font-face 规则，识别每个字体切片所覆盖的 Unicode 范围，然后分析文本内容，确定需要哪些字符。随后，系统仅下载包含这些字符的字体切片，而不是整个字体文件。这种按需加载的策略显著减少数据传输量。

                        How Klyph Works

                        Klyph parses CSS @font-face rules with unicode-range descriptors, analyzes your text to identify required characters, and loads only the necessary font slices on demand. This intelligent approach dramatically reduces data transfer compared to loading entire font files. The system employs global caching with request deduplication, ensuring each font slice is loaded only once even when multiple components request it simultaneously.

                        核心架构优势：

                        1. 直接解析 — CSS 解析器直接生成类型化的字体描述符，无需中间数据结构
                        2. 字符级精确度 — 根据 unicode-range 匹配字符与字体切片，支持多语言混排
                        3. 智能缓存 — 全局缓存配合请求去重机制，避免重复下载和解析
                        4. 作用域 API — 类似 Row、Column 的作用域模式，提供类型安全的接口
                        5. 简单配置 — 只需提供 CSS URL 即可使用

                        Architecture Highlights:

                        • Direct CSS parsing to typed font descriptors without intermediate structures
                        • Character-level precision with per-character font slice assignment
                        • Global caching with request deduplication prevents redundant operations
                        • Scoped API pattern provides type-safe, ergonomic interface
                        • Simple configuration with CSS URL

                        实际应用场景：假设你的应用需要显示一段包含少量汉字的文本。传统方式需要下载数 MB 的完整字体文件。而使用 Klyph，系统会自动识别所需字符，仅下载对应的字体切片，显著减少数据传输量。更重要的是，当用户浏览应用的其他页面时，已加载的字体切片会被复用，快速渲染无需额外下载。

                        Real-World Impact: Consider an app displaying Chinese text. Traditional loading requires downloading entire multi-megabyte font files. Klyph automatically identifies the needed characters and loads only the relevant slices, dramatically reducing data transfer. As users navigate through the app, cached font slices are reused, enabling fast rendering without additional downloads.
                    """.trimIndent(),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        provider = CssUrlFontDescriptorProvider(MoonStarsKaiTRegular)
                    )

                    Text(
                        text = "This large text demonstrates efficient loading of mixed CJK and Latin characters. " +
                                "Only the specific font slices needed for the rendered characters are loaded, " +
                                "not the entire multi-megabyte font file.",
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .align(Alignment.TopEnd)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier)
            Text(
                text = "Stats",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            Text(
                text = "Font Faces from CSS",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            val fontDescriptors by CssCache.descriptors.collectAsState()

            Text(
                text = "Loaded ${fontDescriptors.values.flatten().size} font descriptors from CSS",
                fontSize = 14.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                fontDescriptors.forEach { (url, descriptors) ->
                    Text(
                        text = "From CSS: $url",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    descriptors.take(3).forEach { descriptor ->
                        Text(
                            text = "• ${descriptor.fontFamily} | Weight: ${descriptor.weight.weight} | Style: ${descriptor.style} | Unicode ranges: ${
                                if (descriptor.unicodeRanges.isEmpty()) "all"
                                else descriptor.unicodeRanges.joinToString(", ")
                            }",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }
                    if (descriptors.size > 3) {
                        Text(
                            text = "... and ${descriptors.size - 3} more",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Cache Stats",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            val cachedFontDescriptors by FontSliceCache.descriptors.collectAsState()
            val fontCacheSize = cachedFontDescriptors.size
            val cssCacheSize = fontDescriptors.size

            Text(
                text = "Font cache: $fontCacheSize slices | CSS cache: $cssCacheSize files",
                fontSize = 14.sp
            )

            val cssReceivedBytes by CssCache.receivedBytes.collectAsState()
            val fontReceivedBytes by FontSliceCache.receivedBytes.collectAsState()
            Text(
                text = "Total data received: ${(cssReceivedBytes + fontReceivedBytes) / 1024} KB (estimated)",
                fontSize = 14.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cachedFontDescriptors.forEach { (cacheKey, descriptor) ->
                    Text(
                        text = "• ${descriptor.fontFamily} | Weight: ${descriptor.weight.weight} | Style: ${descriptor.style} | From: $cacheKey",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier)
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            onClick = {
                CssCache.clearAsync()
                FontSliceCache.clearAsync()
            }
        ) {
            Text(
                text = "Clear Cache",
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}

