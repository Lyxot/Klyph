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

/**
 * Represents a Unicode code point range (e.g., U+4E00-9FFF).
 *
 * @param start The starting code point (inclusive).
 * @param end The ending code point (inclusive).
 */
data class UnicodeRange(
    val start: Int,
    val end: Int
) {
    /**
     * Checks if the given character falls within this Unicode range.
     */
    fun contains(char: Char): Boolean {
        val codePoint = char.code
        return codePoint in start..end
    }

    /**
     * Checks if the given code point falls within this Unicode range.
     */
    fun contains(codePoint: Int): Boolean {
        return codePoint in start..end
    }

    override fun toString(): String {
        return "U+${start.toString(16).uppercase()}-${end.toString(16).uppercase()}"
    }
}

/**
 * Parses a CSS unicode-range descriptor value into a list of UnicodeRange objects.
 *
 * Supports formats:
 * - Single code point: "U+26"
 * - Range: "U+0-FF", "U+4E00-9FFF"
 * - Wildcard: "U+4??" (equivalent to U+400-4FF)
 * - Multiple ranges: "U+0-FF, U+131, U+152-153"
 *
 * @param unicodeRangeStr The unicode-range value from CSS (e.g., "U+0-FF, U+4E00-9FFF").
 * @return A list of [UnicodeRange] objects, or an empty list if parsing fails.
 */
fun parseUnicodeRange(unicodeRangeStr: String?): List<UnicodeRange> {
    if (unicodeRangeStr.isNullOrBlank()) {
        return emptyList()
    }

    val ranges = mutableListOf<UnicodeRange>()

    // Split by comma to handle multiple ranges
    val parts = unicodeRangeStr.split(',').map { it.trim() }

    for (part in parts) {
        // Remove "U+" or "u+" prefix
        val cleaned = part.removePrefix("U+").removePrefix("u+").trim()

        if (cleaned.contains('?')) {
            // Wildcard pattern: "4??" means 400-4FF
            val wildcardRange = parseWildcardRange(cleaned)
            if (wildcardRange != null) {
                ranges.add(wildcardRange)
            }
        } else if (cleaned.contains('-')) {
            // Range: "0-FF" or "4E00-9FFF"
            val rangeParts = cleaned.split('-').map { it.trim() }
            if (rangeParts.size == 2) {
                val start = rangeParts[0].toIntOrNull(16)
                val end = rangeParts[1].toIntOrNull(16)
                if (start != null && end != null && start <= end) {
                    ranges.add(UnicodeRange(start, end))
                }
            }
        } else {
            // Single code point: "26" or "131"
            val codePoint = cleaned.toIntOrNull(16)
            if (codePoint != null) {
                ranges.add(UnicodeRange(codePoint, codePoint))
            }
        }
    }

    return ranges
}

/**
 * Parses a wildcard unicode-range pattern (e.g., "4??") into a UnicodeRange.
 *
 * @param wildcardStr The wildcard string without "U+" prefix.
 * @return A [UnicodeRange] or null if parsing fails.
 */
private fun parseWildcardRange(wildcardStr: String): UnicodeRange? {
    // Replace ? with 0 for start, F for end
    val startStr = wildcardStr.replace('?', '0')
    val endStr = wildcardStr.replace('?', 'F')

    val start = startStr.toIntOrNull(16)
    val end = endStr.toIntOrNull(16)

    return if (start != null && end != null && start <= end) {
        UnicodeRange(start, end)
    } else {
        null
    }
}

/**
 * Checks if a character is covered by any of the given unicode ranges.
 *
 * @param char The character to check.
 * @param ranges The list of unicode ranges to check against.
 * @return True if the character falls within at least one range, false otherwise.
 */
fun isCharInRanges(char: Char, ranges: List<UnicodeRange>): Boolean {
    return ranges.any { it.contains(char) }
}

