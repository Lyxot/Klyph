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

class FontDescriptorTest {

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
    fun testFontDescriptorEquality() {
        val descriptor1 = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        val descriptor2 = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        assertEquals(descriptor1, descriptor2)
    }

    @Test
    fun testFontDescriptorInequality() {
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

        assertNotEquals(descriptor1, descriptor2)
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
    fun testFontDescriptorHashCode() {
        val descriptor1 = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        val descriptor2 = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        // Equal objects should have equal hash codes
        assertEquals(descriptor1.hashCode(), descriptor2.hashCode())
    }

    @Test
    fun testFontDescriptorCopy() {
        val original = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        val copy = original.copy(weight = FontWeight.Bold)

        assertEquals(original.url, copy.url)
        assertEquals(original.fontFamily, copy.fontFamily)
        assertNotEquals(original.weight, copy.weight)
        assertEquals(FontWeight.Bold, copy.weight)
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

    @Test
    fun testFontDescriptorUsedAsMapKey() {
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

        val map = mutableMapOf<FontDescriptor, String>()
        map[descriptor1] = "Font 1"
        map[descriptor2] = "Font 2"

        assertEquals(2, map.size)
        assertEquals("Font 1", map[descriptor1])
        assertEquals("Font 2", map[descriptor2])
    }

    @Test
    fun testFontDescriptorListReference() {
        val ranges = mutableListOf(UnicodeRange(0x0, 0xFF))
        val rangeList = UnicodeRangeList(ranges)
        val descriptor = UrlFontDescriptor(
            url = "font.woff2",
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = rangeList
        )

        // Note: FontDescriptor holds a reference to the original list
        // Modifying original list will affect descriptor (current behavior)
        ranges.add(UnicodeRange(0x100, 0x1FF))

        // Descriptor's list reflects the modification
        assertEquals(2, descriptor.unicodeRanges.size)
    }
}
