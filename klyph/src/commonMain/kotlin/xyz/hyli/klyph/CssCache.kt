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

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Global cache for CSS files with request deduplication.
 *
 * Caches parsed CSS font descriptors to avoid redundant network requests
 * and parsing when the same CSS URL is used multiple times.
 *
 * **Request Deduplication:**
 * When multiple concurrent requests are made for the same CSS URL,
 * only one network request and parse operation is performed, and all
 * requests share the result. This prevents duplicate work when multiple
 * composables mount simultaneously.
 *
 * **Thread Safety:**
 * All operations are protected by a mutex to ensure thread-safe access
 * to the cache across multiple coroutines.
 */
object CssCache {
    private val cache = mutableMapOf<String, Deferred<List<ParsedFontDescriptor>>>()
    private val mutex = Mutex()
    private val _size = MutableStateFlow(0)

    /**
     * The current number of cached CSS files.
     */
    val size: StateFlow<Int>
        get() = _size

    /**
     * Gets parsed CSS font descriptors from cache or fetches and parses if not cached.
     *
     * Implements request deduplication: if multiple concurrent calls request
     * the same CSS URL, only one network fetch and parse occurs.
     *
     * @param url The URL of the CSS file.
     * @return List of ParsedFontDescriptor objects parsed from the CSS.
     * @throws Exception if fetching or parsing fails.
     */
    suspend fun getOrLoad(url: String): List<ParsedFontDescriptor> = coroutineScope {
        val deferred = mutex.withLock {
            // Check if already in cache or being fetched
            cache[url]?.let { return@withLock it }

            // Not in cache, create deferred and start fetch
            async {
                try {
                    val res = httpClient.get(url)
                    val body = res.body<String>()
                    parseCssToDescriptors(body, baseUrl = url)
                } catch (e: Exception) {
                    // Remove from cache on error so retry is possible
                    mutex.withLock { cache.remove(url) }
                    throw e
                }
            }.also {
                cache[url] = it
                _size.value = cache.size
            }
        }

        deferred.await()
    }

    /**
     * Clears all cached CSS data.
     */
    suspend fun clear() {
        mutex.withLock {
            cache.clear()
            _size.value = 0
        }
    }
}

/**
 * Fetches and parses CSS font descriptions from a URL.
 *
 * Automatically resolves relative URLs in the CSS against the CSS file's URL.
 * Results are cached globally to avoid redundant requests.
 *
 * This is a convenience function that delegates to [CssCache.getOrLoad].
 *
 * @param url The URL of the CSS file.
 * @return A list of ParsedFontDescriptor objects parsed from the CSS with resolved URLs.
 * @throws Exception if fetching or parsing fails.
 */
suspend fun getFontCssDescription(url: String): List<ParsedFontDescriptor> {
    return CssCache.getOrLoad(url)
}
