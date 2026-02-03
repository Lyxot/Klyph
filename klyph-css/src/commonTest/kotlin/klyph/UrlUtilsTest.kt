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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun testDataUrlFontLoadsWithoutHttp() = runTest {
        val dataUrl = "data:font/woff2;base64,AQIDBA=="
        val descriptor = UrlFontDescriptor(
            url = dataUrl,
            fontFamily = "Data Font",
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            unicodeRanges = UnicodeRangeList()
        )

        var bytesLoaded = 0L
        descriptor.getFontFamily { bytesLoaded = it }

        assertEquals(4L, bytesLoaded)
    }

    @Test
    fun testDataUrlBase64Css() = runTest {
        val dataUrl = "data:text/css;base64," +
                "QGZvbnQtZmFjZSB7CiAgICBmb250LWZhbWlseTogJ0RhdGEgRm9udCc7CiAgICBzcmM6IHVybChodHRwczovL2V4YW1wbGUuY29tL2RhdGEud29mZjIpOwogICAgZm9udC13ZWlnaHQ6IDQwMDsKICAgIGZvbnQtc3R5bGU6IG5vcm1hbDsKICAgIHVuaWNvZGUtcmFuZ2U6IFUrMC1GRjsKfQo="

        val descriptors = CssCache.getOrLoad(dataUrl)

        assertEquals(1, descriptors.size)
        val descriptor = descriptors.first() as UrlFontDescriptor
        assertEquals("Data Font", descriptor.fontFamily)
        assertEquals("https://example.com/data.woff2", descriptor.url)
        assertEquals(FontWeight(400), descriptor.weight)
        assertEquals(FontStyle.Normal, descriptor.style)
        assertEquals(1, descriptor.unicodeRanges.size)
        assertTrue(descriptor.unicodeRanges.contains('A'))
    }

    @Test
    fun testDataUrlPercentEncodedCss() = runTest {
        val dataUrl = "data:text/css," +
                "%40font-face%20%7B%0A%20%20%20%20font-family%3A%20%27Data%20Font%27%3B%0A%20%20%20%20src%3A%20url%28https%3A//example.com/data.woff2%29%3B%0A%20%20%20%20font-weight%3A%20400%3B%0A%20%20%20%20font-style%3A%20normal%3B%0A%20%20%20%20unicode-range%3A%20U%2B0-FF%3B%0A%7D%0A"

        val descriptors = CssCache.getOrLoad(dataUrl)

        assertEquals(1, descriptors.size)
        val descriptor = descriptors.first() as UrlFontDescriptor
        assertEquals("Data Font", descriptor.fontFamily)
        assertEquals("https://example.com/data.woff2", descriptor.url)
        assertEquals(FontWeight(400), descriptor.weight)
        assertEquals(FontStyle.Normal, descriptor.style)
        assertTrue(descriptor.unicodeRanges.contains('A'))
    }
}
