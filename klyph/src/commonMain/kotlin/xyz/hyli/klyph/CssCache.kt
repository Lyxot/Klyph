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

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.hyli.klyph.CssCache.clear

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
    private val _descriptors = MutableStateFlow<Map<String, List<ParsedFontDescriptor>>>(emptyMap())
    private val _receivedBytes = MutableStateFlow(0L)

    /**
     * The list of all parsed font descriptors currently in the cache.
     */
    val descriptors: StateFlow<Map<String, List<ParsedFontDescriptor>>>
        get() = _descriptors

    /**
     * The total number of bytes received from CSS fetches.
     */
    val receivedBytes: StateFlow<Long>
        get() = _receivedBytes

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
                    val body = res.bodyAsText()
                    _receivedBytes.value += res.contentLength() ?: res.bodyAsBytes().size.toLong()
                    parseCssToDescriptors(body, baseUrl = url)
                } catch (e: Exception) {
                    // Remove from cache on error so retry is possible
                    mutex.withLock { cache.remove(url) }
                    throw e
                }
            }.also {
                cache[url] = it
            }
        }

        deferred.await().also {
            _descriptors.value = _descriptors.value.toMutableMap().apply {
                put(url, it)
            }
        }
    }

    /**
     * Clears all cached CSS data.
     */
    suspend fun clear() {
        mutex.withLock {
            cache.clear()
            _descriptors.value = emptyMap()
        }
    }

    /**
     * Calls [clear] in a fire-and-forget way, without suspending.
     *
     * Launches a coroutine on the default dispatcher to clear the cache asynchronously.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun clearAsync() {
        GlobalScope.launch {
            clear()
        }
    }
}
