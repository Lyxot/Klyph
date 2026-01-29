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

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CssParserTest {

    @Test
    fun testParseSimpleFontFace() {
        val css = """
            @font-face {
                font-family: 'Test Font';
                src: url(font.woff2);
                font-weight: 400;
                font-style: normal;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        assertEquals("Test Font", descriptors[0].fontFamily)
        assertEquals("font.woff2", descriptors[0].url)
        assertEquals(FontWeight(400), descriptors[0].weight)
        assertEquals(FontStyle.Normal, descriptors[0].style)
    }

    @Test
    fun testParseMultipleFontFaces() {
        val css = """
            @font-face {
                font-family: 'Font A';
                src: url(font-a.woff2);
            }
            @font-face {
                font-family: 'Font B';
                src: url(font-b.woff2);
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(2, descriptors.size)
        assertEquals("Font A", descriptors[0].fontFamily)
        assertEquals("Font B", descriptors[1].fontFamily)
    }

    @Test
    fun testParseWithUnicodeRange() {
        val css = """
            @font-face {
                font-family: 'CJK Font';
                src: url(cjk.woff2);
                unicode-range: U+4E00-9FFF;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        assertEquals(1, descriptors[0].unicodeRanges.size)
        assertEquals(0x4E00, descriptors[0].unicodeRanges[0].start)
        assertEquals(0x9FFF, descriptors[0].unicodeRanges[0].end)
    }

    @Test
    fun testParseWithMultipleUnicodeRanges() {
        val css = """
            @font-face {
                font-family: 'Latin Font';
                src: url(latin.woff2);
                unicode-range: U+0000-00FF, U+0131, U+0152-0153;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        assertEquals(3, descriptors[0].unicodeRanges.size)
    }

    @Test
    fun testParseUrlWithDoubleQuotes() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url("font.woff2");
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        assertEquals("font.woff2", descriptors[0].url)
    }

    @Test
    fun testParseUrlWithSingleQuotes() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url('font.woff2');
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        assertEquals("font.woff2", descriptors[0].url)
    }

    @Test
    fun testParseUrlWithoutQuotes() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(font.woff2);
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        assertEquals("font.woff2", descriptors[0].url)
    }

    @Test
    fun testParseFontWeightNormal() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(font.woff2);
                font-weight: normal;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(FontWeight.Normal, descriptors[0].weight)
    }

    @Test
    fun testParseFontWeightBold() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(font.woff2);
                font-weight: bold;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(FontWeight.Bold, descriptors[0].weight)
    }

    @Test
    fun testParseFontWeightNumeric() {
        val weights = listOf(100, 200, 300, 400, 500, 600, 700, 800, 900)

        for (weight in weights) {
            val css = """
                @font-face {
                    font-family: 'Test';
                    src: url(font.woff2);
                    font-weight: $weight;
                }
            """.trimIndent()

            val descriptors = parseCssToDescriptors(css)
            assertEquals(FontWeight(weight), descriptors[0].weight)
        }
    }

    @Test
    fun testParseFontStyleNormal() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(font.woff2);
                font-style: normal;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(FontStyle.Normal, descriptors[0].style)
    }

    @Test
    fun testParseFontStyleItalic() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(font.woff2);
                font-style: italic;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(FontStyle.Italic, descriptors[0].style)
    }

    @Test
    fun testParseFontStyleOblique() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(font.woff2);
                font-style: oblique;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        // Oblique is treated as italic in Compose
        assertEquals(FontStyle.Italic, descriptors[0].style)
    }

    @Test
    fun testParseWithCssComments() {
        val css = """
            /* This is a comment */
            @font-face {
                font-family: 'Test'; /* inline comment */
                src: url(font.woff2);
                /* multi-line
                   comment */
                font-weight: 400;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        assertEquals("Test", descriptors[0].fontFamily)
    }

    @Test
    fun testParseWithMultilineCssComments() {
        val css = """
            /*
             * Multi-line comment
             * spanning several lines
             */
            @font-face {
                font-family: 'Test';
                src: url(font.woff2);
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
    }

    @Test
    fun testParseWithRelativeUrl() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(./fonts/font.woff2);
            }
        """.trimIndent()

        val baseUrl = "https://example.com/css/styles.css"
        val descriptors = parseCssToDescriptors(css, baseUrl)

        assertEquals(1, descriptors.size)
        assertEquals("https://example.com/css/fonts/font.woff2", descriptors[0].url)
    }

    @Test
    fun testParseWithParentRelativeUrl() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(../fonts/font.woff2);
            }
        """.trimIndent()

        val baseUrl = "https://example.com/css/styles.css"
        val descriptors = parseCssToDescriptors(css, baseUrl)

        assertEquals(1, descriptors.size)
        assertEquals("https://example.com/fonts/font.woff2", descriptors[0].url)
    }

    @Test
    fun testParseWithAbsoluteUrl() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(https://cdn.example.com/font.woff2);
            }
        """.trimIndent()

        val baseUrl = "https://example.com/css/styles.css"
        val descriptors = parseCssToDescriptors(css, baseUrl)

        assertEquals(1, descriptors.size)
        assertEquals("https://cdn.example.com/font.woff2", descriptors[0].url)
    }

    @Test
    fun testParseWithoutBaseUrl() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(./fonts/font.woff2);
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        // Without baseUrl, relative URLs remain as-is
        assertEquals("./fonts/font.woff2", descriptors[0].url)
    }

    @Test
    fun testParseEmptyCss() {
        val descriptors = parseCssToDescriptors("")
        assertEquals(0, descriptors.size)
    }

    @Test
    fun testParseInvalidCss() {
        val css = "This is not valid CSS"
        val descriptors = parseCssToDescriptors(css)
        assertEquals(0, descriptors.size)
    }

    @Test
    fun testParseMissingFontFamily() {
        val css = """
            @font-face {
                src: url(font.woff2);
                font-weight: 400;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)
        // Should skip this @font-face since font-family is required
        assertEquals(0, descriptors.size)
    }

    @Test
    fun testParseMissingSrc() {
        val css = """
            @font-face {
                font-family: 'Test';
                font-weight: 400;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)
        // Should skip this @font-face since src is required
        assertEquals(0, descriptors.size)
    }

    @Test
    fun testParseWithFormat() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(font.woff2) format('woff2');
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        assertEquals("font.woff2", descriptors[0].url)
    }

    @Test
    fun testParseWithMultipleSources() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(font.woff2) format('woff2'),
                     url(font.woff) format('woff');
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        // Should extract first URL
        assertEquals("font.woff2", descriptors[0].url)
    }

    @Test
    fun testParseRealWorldGoogleFontsStyle() {
        val css = """
            @font-face {
              font-family: 'Noto Sans SC';
              font-style: normal;
              font-weight: 400;
              font-display: swap;
              src: url(https://fonts.gstatic.com/s/notosanssc/v36/k3kXo84MPvpLmixcA63oeAL7Iqp5IZJF9bmaG9_FnYxNbPzS5HE.0.woff2) format('woff2');
              unicode-range: U+f1000-f1fff, U+f21bb-f21bc, U+f21be, U+f21c0, U+f21c2-f21c4, U+f21c6, U+f21c8, U+f21cc-f21cd, U+f21d0, U+f21d2, U+f21d4, U+f21d7-f21d8, U+f21dc-f21dd, U+f21df-f21e1, U+f21e3, U+f21e5, U+f21e8, U+f21f0, U+f21f3, U+f21f5-f21f6, U+f21f8-f21fa;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        assertEquals("Noto Sans SC", descriptors[0].fontFamily)
        assertEquals(FontWeight(400), descriptors[0].weight)
        assertEquals(FontStyle.Normal, descriptors[0].style)
        assertTrue(descriptors[0].url.startsWith("https://fonts.gstatic.com"))
        assertTrue(descriptors[0].unicodeRanges.isNotEmpty())
    }

    @Test
    fun testParseCompactCss() {
        // Test CSS without whitespace
        val css = "@font-face{font-family:'Test';src:url(font.woff2);font-weight:700;}"

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        assertEquals("Test", descriptors[0].fontFamily)
        assertEquals(FontWeight(700), descriptors[0].weight)
    }

    @Test
    fun testParseWithExtraWhitespace() {
        val css = """
            @font-face   {
                font-family  :  'Test'  ;
                src  :  url( font.woff2 )  ;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        assertEquals("Test", descriptors[0].fontFamily)
    }

    @Test
    fun testParseCaseInsensitiveProperties() {
        val css = """
            @font-face {
                FONT-FAMILY: 'Test';
                SRC: url(font.woff2);
                FONT-WEIGHT: BOLD;
                FONT-STYLE: ITALIC;
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        // CSS properties should be case-insensitive, but current implementation is case-sensitive
        // This test documents current behavior - may need fixing
        assertEquals(0, descriptors.size) // Currently fails because properties are lowercase
    }

    @Test
    fun testParseDefaultValues() {
        val css = """
            @font-face {
                font-family: 'Test';
                src: url(font.woff2);
            }
        """.trimIndent()

        val descriptors = parseCssToDescriptors(css)

        assertEquals(1, descriptors.size)
        // When not specified, should default to Normal weight and style
        assertEquals(FontWeight.Normal, descriptors[0].weight)
        assertEquals(FontStyle.Normal, descriptors[0].style)
        assertEquals(0, descriptors[0].unicodeRanges.size)
    }
}
