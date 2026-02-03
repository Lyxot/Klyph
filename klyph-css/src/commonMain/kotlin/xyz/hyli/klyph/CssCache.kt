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
import xyz.hyli.klyph.CssCache.getOrLoad

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
    private val cache = mutableMapOf<String, Deferred<List<FontDescriptor>>>()
    private val mutex = Mutex()
    private val _descriptors = MutableStateFlow<Map<String, List<FontDescriptor>>>(emptyMap())
    private val _receivedBytes = MutableStateFlow(0L)

    /**
     * The list of all parsed font descriptors currently in the cache.
     */
    val descriptors: StateFlow<Map<String, List<FontDescriptor>>>
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
     * @return List of FontDescriptor objects parsed from the CSS.
     * @throws Exception if fetching or parsing fails.
     */
    suspend fun getOrLoad(url: String): List<FontDescriptor> =
        getOrLoad("url:$url") {
            if (url.startsWith("data:", ignoreCase = true)) {
                val data = decodeDataUrlToBytes(url)
                _receivedBytes.value += data.size.toLong()
                parseCssToDescriptors(data.decodeToString(), baseUrl = "")
            } else {
                val res = httpClient.get(url)
                val body = res.bodyAsText()
                _receivedBytes.value += res.contentLength() ?: res.bodyAsBytes().size.toLong()
                parseCssToDescriptors(body, baseUrl = url)
            }
        }

    /**
     * Gets parsed CSS font descriptors from CSS content string, with caching.
     *
     * Uses a hash-based cache key to avoid reparsing the same CSS content.
     *
     * NOTE: This is a best-effort cache key and can theoretically collide for
     * different CSS payloads. We accept this risk to keep the key short and
     * computation fast; call sites that require strict content identity should
     * avoid relying on content-based caching.
     *
     * @param cssContent The raw CSS content string.
     * @param baseUrl The base URL for resolving relative URLs in the CSS.
     * @return List of FontDescriptor objects parsed from the CSS.
     * @throws Exception if parsing fails.
     */
    suspend fun getOrLoad(cssContent: String, baseUrl: String): List<FontDescriptor> =
        getOrLoad("hash:${"$cssContent:$baseUrl".toFnv1aHashString()}") {
            parseCssToDescriptors(cssContent, baseUrl)
        }

    /**
     * Internal helper function that implements caching and request deduplication for CSS parsing.
     *
     * This function is the core caching mechanism used by both [getOrLoad] and [getOrLoad].
     * It ensures that:
     * - Multiple concurrent requests for the same cache key share a single parse operation
     * - Results are cached for subsequent requests
     * - Failed operations are removed from cache to allow retries
     *
     * **Request Deduplication:**
     * When multiple concurrent calls request the same cache key, only one parse operation
     * is executed via the [parseBlock], and all callers await the same [Deferred] result.
     *
     * **Cache Key:**
     * - For URL-based CSS: "url:" prefix and the URL itself is used as the cache key
     * - For content-based CSS: a hash-based key in the format "hash:{hashValue}"
     *
     * @param cacheKey Unique key for this cache entry (URL or hash-based key for content)
     * @param parseBlock Suspend function that performs the actual parsing/fetching operation
     * @return List of FontDescriptor objects from cache or newly parsed
     * @throws Exception if the parseBlock throws, the error is propagated and cache entry is removed
     */
    private suspend fun getOrLoad(
        cacheKey: String,
        parseBlock: suspend () -> List<FontDescriptor>
    ): List<FontDescriptor> = coroutineScope {
        val deferred = mutex.withLock {
            // Check if already in cache or being parsed
            cache[cacheKey]?.let { return@withLock it }

            // Not in cache, create deferred and start parse
            async {
                try {
                    parseBlock()
                } catch (e: Exception) {
                    // Remove from cache on error so retry is possible
                    mutex.withLock { cache.remove(cacheKey) }
                    throw e
                }
            }.also {
                cache[cacheKey] = it
            }
        }

        deferred.await().also {
            _descriptors.value = _descriptors.value.toMutableMap().apply {
                put(cacheKey, it)
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
