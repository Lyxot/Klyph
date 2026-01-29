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
 * Interface for providing parsed font descriptors from various sources.
 *
 * Implementations can load descriptors from CSS URLs, CSS content strings,
 * or any other source.
 */
interface FontDescriptorProvider {
    /**
     * Loads and returns the list of parsed font descriptors.
     *
     * This function is called internally to fetch descriptors from the provider's source.
     *
     * @return List of parsed font descriptors.
     */
    suspend fun getDescriptors(): List<FontDescriptor>
}

/**
 * Implementation of FontDescriptorProvider that provides a static list of descriptors.
 *
 * This provider is useful for bundled fonts loaded from Compose resources
 * or when you have pre-constructed font descriptors that don't need fetching.
 *
 * Example:
 * ```kotlin
 * val descriptor = ResourceFontDescriptor(
 *     resource = Res.font.my_font,
 *     fontFamily = "MyFont",
 *     weight = FontWeight.Normal,
 *     style = FontStyle.Normal,
 *     unicodeRanges = listOf(UnicodeRange(0x0, 0xFF))
 * )
 * val provider = StaticFontDescriptorProvider(descriptor)
 * SubsetFontProvider(provider = provider) {
 *     SubsetText("Hello")
 * }
 * ```
 *
 * @param descriptors The list of font descriptors to provide.
 */
class StaticFontDescriptorProvider(
    private val descriptors: List<FontDescriptor>
) : FontDescriptorProvider {
    constructor(
        vararg descriptors: FontDescriptor
    ) : this(descriptors.toList())

    override suspend fun getDescriptors(): List<FontDescriptor> = descriptors
}
