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

class UrlUtilsTest {

    @Test
    fun testResolveAbsoluteUrl() {
        val baseUrl = "https://example.com/styles/fonts.css"
        val absoluteUrl = "https://cdn.example.com/font.woff2"

        val result = resolveUrl(baseUrl, absoluteUrl)

        assertEquals("https://cdn.example.com/font.woff2", result)
    }

    @Test
    fun testResolveRelativeUrlSameDirectory() {
        val baseUrl = "https://example.com/styles/fonts.css"
        val relativeUrl = "font.woff2"

        val result = resolveUrl(baseUrl, relativeUrl)

        assertEquals("https://example.com/styles/font.woff2", result)
    }

    @Test
    fun testResolveRelativeUrlCurrentDirectory() {
        val baseUrl = "https://example.com/styles/fonts.css"
        val relativeUrl = "./font.woff2"

        val result = resolveUrl(baseUrl, relativeUrl)

        assertEquals("https://example.com/styles/font.woff2", result)
    }

    @Test
    fun testResolveRelativeUrlParentDirectory() {
        val baseUrl = "https://example.com/styles/fonts.css"
        val relativeUrl = "../fonts/font.woff2"

        val result = resolveUrl(baseUrl, relativeUrl)

        assertEquals("https://example.com/fonts/font.woff2", result)
    }

    @Test
    fun testResolveRelativeUrlMultipleParentDirectories() {
        val baseUrl = "https://example.com/a/b/c/fonts.css"
        val relativeUrl = "../../fonts/font.woff2"

        val result = resolveUrl(baseUrl, relativeUrl)

        assertEquals("https://example.com/a/fonts/font.woff2", result)
    }

    @Test
    fun testResolveAbsolutePath() {
        val baseUrl = "https://example.com/styles/fonts.css"
        val absolutePath = "/assets/fonts/font.woff2"

        val result = resolveUrl(baseUrl, absolutePath)

        assertEquals("https://example.com/assets/fonts/font.woff2", result)
    }

    @Test
    fun testResolveProtocolRelativeUrl() {
        val baseUrl = "https://example.com/styles/fonts.css"
        val protocolRelativeUrl = "//cdn.example.com/font.woff2"

        val result = resolveUrl(baseUrl, protocolRelativeUrl)

        // Current behavior: protocol-relative URLs are returned as-is
        // They will be resolved by the browser when used
        assertEquals("//cdn.example.com/font.woff2", result)
    }

    @Test
    fun testResolveDataUrl() {
        val baseUrl = "https://example.com/styles/fonts.css"
        val dataUrl = "data:font/woff2;base64,d09GMgABAAAAAA..."

        val result = resolveUrl(baseUrl, dataUrl)

        assertEquals("data:font/woff2;base64,d09GMgABAAAAAA...", result)
    }

    @Test
    fun testResolveComplexRelativePath() {
        val baseUrl = "https://example.com/styles/css/fonts.css"
        val relativeUrl = "../assets/fonts/MiSans-Normal.woff2"

        val result = resolveUrl(baseUrl, relativeUrl)

        assertEquals("https://example.com/styles/assets/fonts/MiSans-Normal.woff2", result)
    }

    @Test
    fun testResolveWithHttpProtocol() {
        val baseUrl = "http://example.com/fonts.css"
        val relativeUrl = "./font.woff2"

        val result = resolveUrl(baseUrl, relativeUrl)

        assertEquals("http://example.com/font.woff2", result)
    }

    @Test
    fun testResolveRootDirectory() {
        val baseUrl = "https://example.com/fonts.css"
        val relativeUrl = "font.woff2"

        val result = resolveUrl(baseUrl, relativeUrl)

        assertEquals("https://example.com/font.woff2", result)
    }
}
