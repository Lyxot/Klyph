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
import kotlin.test.assertNotEquals

class UrlFontDescriptorTest {

    @Test
    fun testFontDescriptorCreation() {
        val descriptor = UrlFontDescriptor(
            url = "https://example.com/font.woff2",
            fontFamily = "Test Font",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        assertEquals("https://example.com/font.woff2", descriptor.url)
        assertEquals("Test Font", descriptor.fontFamily)
        assertEquals(FontWeight.Normal, descriptor.weight)
        assertEquals(FontStyle.Normal, descriptor.style)
        assertEquals(0, descriptor.unicodeRanges.size)
    }

    @Test
    fun testFontDescriptorWithUnicodeRanges() {
        val ranges = UnicodeRangeList(
            UnicodeRange(0x0000, 0x00FF),
            UnicodeRange(0x4E00, 0x9FFF)
        )

        val descriptor = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Multi-Script Font",
            weight = FontWeight(400),
            style = FontStyle.Normal,
            unicodeRanges = ranges
        )

        assertEquals(2, descriptor.unicodeRanges.size)
        assertEquals(0x0000, descriptor.unicodeRanges[0].start)
        assertEquals(0x9FFF, descriptor.unicodeRanges[1].end)
    }

    @Test
    fun testFontDescriptorWithDifferentWeights() {
        val descriptorNormal = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        val descriptorBold = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight.Bold,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        assertNotEquals(descriptorNormal, descriptorBold)
    }

    @Test
    fun testFontDescriptorWithDifferentStyles() {
        val descriptorNormal = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        val descriptorItalic = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Italic,
            unicodeRanges = UnicodeRangeList()
        )

        assertNotEquals(descriptorNormal, descriptorItalic)
    }

    @Test
    fun testFontDescriptorWithCustomWeight() {
        val descriptor = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight(350),
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        assertEquals(350, descriptor.weight.weight)
    }

    @Test
    fun testCreateFontFromDataGeneratesUniqueIdentity() {
        val descriptor1 = UrlFontDescriptor(
            url = "font1.woff2",
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        val descriptor2 = UrlFontDescriptor(
            url = "font2.woff2",
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        // Create dummy font data
        val dummyData = ByteArray(100) { it.toByte() }

        val font1 = createFontFromData(dummyData, descriptor1)
        val font2 = createFontFromData(dummyData, descriptor2)

        // Fonts should have different identities based on different URLs
        assertNotEquals(font1, font2)
    }

    @Test
    fun testCreateFontFromDataPreservesWeight() {
        val descriptor = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight.Bold,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        val dummyData = ByteArray(100) { it.toByte() }
        val font = createFontFromData(dummyData, descriptor)

        assertEquals(FontWeight.Bold, font.weight)
    }

    @Test
    fun testCreateFontFromDataPreservesStyle() {
        val descriptor = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Italic,
            unicodeRanges = UnicodeRangeList()
        )

        val dummyData = ByteArray(100) { it.toByte() }
        val font = createFontFromData(dummyData, descriptor)

        assertEquals(FontStyle.Italic, font.style)
    }

}
