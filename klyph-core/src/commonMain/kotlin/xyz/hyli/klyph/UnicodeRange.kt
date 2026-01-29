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
 * Checks if a character is covered by any of the given Unicode ranges.
 *
 * @param char The character to check.
 * @param ranges The list of Unicode ranges to check against.
 * @return True if the character falls within at least one range, false otherwise.
 */
fun isCharInRanges(char: Char, ranges: List<UnicodeRange>): Boolean {
    return ranges.any { it.contains(char) }
}
