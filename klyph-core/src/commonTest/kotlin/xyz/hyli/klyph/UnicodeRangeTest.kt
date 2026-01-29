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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnicodeRangeTest {

    @Test
    fun testParseSingleCodePoint() {
        val ranges = parseUnicodeRange("U+26")

        assertEquals(1, ranges.size)
        assertEquals(0x26, ranges[0].start)
        assertEquals(0x26, ranges[0].end)
    }

    @Test
    fun testParseRange() {
        val ranges = parseUnicodeRange("U+4E00-9FFF")

        assertEquals(1, ranges.size)
        assertEquals(0x4E00, ranges[0].start)
        assertEquals(0x9FFF, ranges[0].end)
    }

    @Test
    fun testParseWildcard() {
        val ranges = parseUnicodeRange("U+4??")

        assertEquals(1, ranges.size)
        assertEquals(0x400, ranges[0].start)
        assertEquals(0x4FF, ranges[0].end)
    }

    @Test
    fun testParseMultipleWildcards() {
        val ranges = parseUnicodeRange("U+4???")

        assertEquals(1, ranges.size)
        assertEquals(0x4000, ranges[0].start)
        assertEquals(0x4FFF, ranges[0].end)
    }

    @Test
    fun testParseMultipleRanges() {
        val ranges = parseUnicodeRange("U+0-FF, U+131, U+152-153")

        assertEquals(3, ranges.size)

        // First range: U+0-FF
        assertEquals(0x0, ranges[0].start)
        assertEquals(0xFF, ranges[0].end)

        // Second range: U+131 (single code point)
        assertEquals(0x131, ranges[1].start)
        assertEquals(0x131, ranges[1].end)

        // Third range: U+152-153
        assertEquals(0x152, ranges[2].start)
        assertEquals(0x153, ranges[2].end)
    }

    @Test
    fun testParseLowercasePrefix() {
        val ranges = parseUnicodeRange("u+4e00-9fff")

        assertEquals(1, ranges.size)
        assertEquals(0x4E00, ranges[0].start)
        assertEquals(0x9FFF, ranges[0].end)
    }

    @Test
    fun testParseEmptyString() {
        val ranges = parseUnicodeRange("")
        assertEquals(0, ranges.size)
    }

    @Test
    fun testParseNullString() {
        val ranges = parseUnicodeRange(null)
        assertEquals(0, ranges.size)
    }

    @Test
    fun testParseInvalidFormat() {
        // Invalid format should return empty list
        val ranges = parseUnicodeRange("invalid")
        assertEquals(0, ranges.size)
    }

    @Test
    fun testParseWithSpaces() {
        val ranges = parseUnicodeRange("U+0-FF , U+131 , U+152-153")

        assertEquals(3, ranges.size)
        assertEquals(0x0, ranges[0].start)
        assertEquals(0xFF, ranges[0].end)
    }

    @Test
    fun testUnicodeRangeContainsChar() {
        val range = UnicodeRange(0x4E00, 0x9FFF)

        assertTrue(range.contains('一')) // U+4E00
        assertTrue(range.contains('中')) // U+4E2D
        assertTrue(range.contains('龥')) // U+9FA5
        assertFalse(range.contains('A')) // U+0041
        assertFalse(range.contains('a')) // U+0061
    }

    @Test
    fun testUnicodeRangeContainsCodePoint() {
        val range = UnicodeRange(0x0, 0xFF)

        assertTrue(range.contains(0x00))
        assertTrue(range.contains(0x41)) // 'A'
        assertTrue(range.contains(0xFF))
        assertFalse(range.contains(0x100))
        assertFalse(range.contains(0x4E00))
    }

    @Test
    fun testIsCharInRanges() {
        val ranges = listOf(
            UnicodeRange(0x0, 0xFF),      // Basic Latin + Latin-1 Supplement
            UnicodeRange(0x4E00, 0x9FFF)  // CJK Unified Ideographs
        )

        // Test Basic Latin characters
        assertTrue(isCharInRanges('A', ranges))
        assertTrue(isCharInRanges('z', ranges))
        assertTrue(isCharInRanges('0', ranges))

        // Test CJK characters
        assertTrue(isCharInRanges('你', ranges))
        assertTrue(isCharInRanges('好', ranges))
        assertTrue(isCharInRanges('世', ranges))

        // Test Latin-1 Supplement characters (in range)
        assertTrue(isCharInRanges('ñ', ranges)) // U+00F1 (within 0x0-0xFF)

        // Test characters not in any range
        assertFalse(isCharInRanges('Ā', ranges)) // U+0100 (outside 0xFF)
        assertFalse(isCharInRanges('Ə', ranges)) // U+018F (outside ranges)
    }

    @Test
    fun testIsCharInRangesWithEmptyList() {
        assertFalse(isCharInRanges('A', emptyList()))
        assertFalse(isCharInRanges('你', emptyList()))
    }

    @Test
    fun testUnicodeRangeToString() {
        val range = UnicodeRange(0x4E00, 0x9FFF)
        assertEquals("U+4E00-9FFF", range.toString())
    }

    @Test
    fun testSingleCodePointToString() {
        val range = UnicodeRange(0x26, 0x26)
        assertEquals("U+26-26", range.toString())
    }

    @Test
    fun testParseLatinRange() {
        val ranges = parseUnicodeRange("U+0020-007E")

        assertEquals(1, ranges.size)
        assertEquals(0x20, ranges[0].start)  // Space
        assertEquals(0x7E, ranges[0].end)    // Tilde ~
    }

    @Test
    fun testParseJapaneseHiragana() {
        val ranges = parseUnicodeRange("U+3040-309F")

        assertEquals(1, ranges.size)
        assertTrue(ranges[0].contains('あ'))
        assertTrue(ranges[0].contains('ん'))
        assertFalse(ranges[0].contains('ア')) // Katakana, different range
    }

    @Test
    fun testParseJapaneseKatakana() {
        val ranges = parseUnicodeRange("U+30A0-30FF")

        assertEquals(1, ranges.size)
        assertTrue(ranges[0].contains('ア'))
        assertTrue(ranges[0].contains('ン'))
        assertFalse(ranges[0].contains('あ')) // Hiragana, different range
    }

    @Test
    fun testParseKoreanHangul() {
        val ranges = parseUnicodeRange("U+AC00-D7AF")

        assertEquals(1, ranges.size)
        assertTrue(ranges[0].contains('가'))
        assertTrue(ranges[0].contains('힣'))
    }

    @Test
    fun testParseGoogleFontsStyleRange() {
        // Example from Google Fonts CSS
        val ranges =
            parseUnicodeRange("U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+0304, U+0308, U+0329, U+2000-206F, U+2074, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD")

        // Should parse all ranges
        assertTrue(ranges.size >= 10)

        // Verify some key ranges
        assertTrue(ranges.any { it.start == 0x0000 && it.end == 0x00FF })
        assertTrue(ranges.any { it.start == 0x0131 && it.end == 0x0131 })
        assertTrue(ranges.any { it.start == 0x0152 && it.end == 0x0153 })
    }

    @Test
    fun testParseInvalidRangeReversed() {
        // Start > End should be ignored
        val ranges = parseUnicodeRange("U+FF-00")
        assertEquals(0, ranges.size)
    }

    @Test
    fun testParseHexadecimalCaseInsensitive() {
        val rangesUpper = parseUnicodeRange("U+4E00-9FFF")
        val rangesLower = parseUnicodeRange("U+4e00-9fff")
        val rangesMixed = parseUnicodeRange("U+4E00-9fff")

        assertEquals(rangesUpper[0].start, rangesLower[0].start)
        assertEquals(rangesUpper[0].end, rangesLower[0].end)
        assertEquals(rangesUpper[0].start, rangesMixed[0].start)
        assertEquals(rangesUpper[0].end, rangesMixed[0].end)
    }
}
