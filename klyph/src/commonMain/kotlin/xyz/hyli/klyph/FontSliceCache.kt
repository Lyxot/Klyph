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

import androidx.compose.ui.text.font.Font
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.hyli.klyph.FontSliceCache.clear

/**
 * Global cache for loaded font slices with request deduplication.
 *
 * This cache stores font file data (as ByteArray) to avoid redundant network
 * requests when the same font slice is needed by multiple composables.
 *
 * **Request Deduplication:**
 * When multiple concurrent requests are made for the same font URL,
 * only one network request is performed and all requests share the result.
 * This prevents duplicate downloads when multiple composables mount simultaneously
 * and request the same font slices.
 *
 * **Thread Safety:**
 * All operations are protected by a mutex to ensure thread-safe access
 * to the cache across multiple coroutines.
 *
 * **Example:**
 * If 10 SubsetText instances all render "你好" and mount at the same time,
 * they will all request the same Chinese font slice. Without deduplication,
 * this would trigger 10 identical network requests. With deduplication,
 * only 1 request is made and all 10 instances share the result.
 */
object FontSliceCache {
    private val cache = mutableMapOf<String, Deferred<Font>>()
    private val mutex = Mutex()
    private val _descriptors = MutableStateFlow<Map<String, ParsedFontDescriptor>>(emptyMap())

    /**
     * The list of all parsed font descriptors currently in the cache.
     */
    val descriptors: StateFlow<Map<String, ParsedFontDescriptor>>
        get() = _descriptors

    /**
     * Gets font data from cache or loads it from the URL if not cached.
     *
     * Implements request deduplication: if multiple concurrent calls request
     * the same URL, only one network fetch occurs and all callers receive
     * the same result.
     *
     * @param url The URL of the font resource.
     * @return The font data as ByteArray.
     * @throws Exception if fetching fails.
     */
    suspend fun getOrLoad(descriptor: ParsedFontDescriptor): Font = coroutineScope {
        val url = descriptor.url
        val deferred = mutex.withLock {
            // Check if already in cache or being fetched
            cache[url]?.let { return@withLock it }

            // Not in cache, create deferred and start fetch
            async {
                try {
                    val res = httpClient.get(url)
                    val fontData = res.bodyAsBytes()
                    createFontFromData(fontData, descriptor)
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
                put(url, descriptor)
            }
        }
    }

    /**
     * Preloads multiple font descriptors into the cache.
     *
     * This can be useful for warming the cache with commonly-used font slices
     * before they're actually needed, improving perceived performance.
     *
     * @param descriptors The list of font descriptors to preload.
     */
    suspend fun preload(descriptors: List<ParsedFontDescriptor>) {
        descriptors.forEach { descriptor ->
            try {
                getOrLoad(descriptor)
            } catch (e: Exception) {
                println("ERROR: Failed to preload font from ${descriptor.url}: ${e.message}")
            }
        }
    }

    /**
     * Clears all cached font data.
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
