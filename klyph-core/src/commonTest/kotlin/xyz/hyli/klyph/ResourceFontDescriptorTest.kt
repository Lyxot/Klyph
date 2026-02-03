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
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResourceFontDescriptorTest {

    @OptIn(InternalResourceApi::class)
    private fun dummyFontResource(id: String = "dummy-font"): FontResource {
        val item = ResourceItem(
            qualifiers = emptySet(),
            path = "dummy.ttf",
            offset = -1,
            size = -1
        )
        return FontResource(id, setOf(item))
    }

    @Test
    fun testCssStringConstructorParsesValues() {
        val descriptor = ResourceFontDescriptor(
            resource = dummyFontResource(),
            fontFamily = "Test Font",
            weight = "400",
            style = "italic",
            unicodeRanges = "U+0-FF, U+131"
        )

        assertEquals(FontWeight(400), descriptor.weight)
        assertEquals(FontStyle.Italic, descriptor.style)
        assertEquals(2, descriptor.unicodeRanges.size)
        assertTrue(descriptor.unicodeRanges.contains('A'))
    }

    @Test
    fun testResourceFontDescriptorEquality() {
        val descriptor1 = ResourceFontDescriptor(
            resource = dummyFontResource("font-a"),
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        val descriptor2 = ResourceFontDescriptor(
            resource = dummyFontResource("font-a"),
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        assertEquals(descriptor1, descriptor2)
    }

    @Test
    fun testResourceFontDescriptorInequality() {
        val descriptor1 = ResourceFontDescriptor(
            resource = dummyFontResource("font-a"),
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        val descriptor2 = ResourceFontDescriptor(
            resource = dummyFontResource("font-b"),
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        assertTrue(descriptor1 != descriptor2)
    }

    @Test
    fun testResourceFontDescriptorHashCode() {
        val descriptor1 = ResourceFontDescriptor(
            resource = dummyFontResource("font-a"),
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        val descriptor2 = ResourceFontDescriptor(
            resource = dummyFontResource("font-a"),
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        assertEquals(descriptor1.hashCode(), descriptor2.hashCode())
    }

    @Test
    fun testResourceFontDescriptorCopy() {
        val original = ResourceFontDescriptor(
            resource = dummyFontResource("font-a"),
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        val copy = original.copy(weight = FontWeight.Bold)

        assertEquals(original.resource, copy.resource)
        assertEquals(original.fontFamily, copy.fontFamily)
        assertEquals(FontWeight.Bold, copy.weight)
    }

    @Test
    fun testResourceFontDescriptorUsedAsMapKey() {
        val descriptor1 = ResourceFontDescriptor(
            resource = dummyFontResource("font-a"),
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        val descriptor2 = ResourceFontDescriptor(
            resource = dummyFontResource("font-b"),
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
    fun testResourceFontDescriptorListReference() {
        val ranges = mutableListOf(UnicodeRange(0x0, 0xFF))
        val rangeList = UnicodeRangeList(ranges)
        val descriptor = ResourceFontDescriptor(
            resource = dummyFontResource("font-a"),
            fontFamily = "Test",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = rangeList
        )

        ranges.add(UnicodeRange(0x100, 0x1FF))

        assertEquals(2, descriptor.unicodeRanges.size)
    }
}
