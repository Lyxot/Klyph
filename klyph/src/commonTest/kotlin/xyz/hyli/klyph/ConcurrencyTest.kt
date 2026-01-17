package xyz.hyli.klyph

import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for request deduplication in caching systems.
 *
 * These tests verify that concurrent requests for the same resource
 * result in only a single fetch operation.
 */
class ConcurrencyTest {

    /**
     * Demonstrates that request deduplication works correctly.
     *
     * This test simulates what happens when multiple composables
     * mount simultaneously and all request the same CSS or font URL.
     *
     * Without deduplication: N concurrent requests = N network fetches
     * With deduplication: N concurrent requests = 1 network fetch
     */
    @Test
    fun testRequestDeduplicationConcept() = runTest {
        // Simulate a counter for network requests
        var fetchCount = 0

        // Simulated fetch function that tracks how many times it's called
        suspend fun simulatedFetch(url: String): String {
            fetchCount++
            // Simulate network delay
            kotlinx.coroutines.delay(100)
            return "result-$url"
        }

        // Simple cache with request deduplication
        val cache = mutableMapOf<String, kotlinx.coroutines.Deferred<String>>()

        suspend fun getOrLoad(url: String): String {
            val deferred = synchronized(cache) {
                // Check if already in cache
                cache[url]?.let { return@synchronized it }

                // Not in cache, create deferred and start fetch
                async {
                    val result = simulatedFetch(url)
                    result
                }.also { cache[url] = it }
            }

            return deferred.await()
        }

        // Launch 10 concurrent requests for the same URL
        val requests = (1..10).map {
            async {
                getOrLoad("same-url")
            }
        }

        // Wait for all requests to complete
        val results = requests.awaitAll()

        // Verify all requests got the same result
        assertEquals("result-same-url", results.first())
        assertEquals(10, results.size)
        results.forEach { assertEquals("result-same-url", it) }

        // CRITICAL: Verify only 1 fetch happened despite 10 concurrent requests
        assertEquals(1, fetchCount, "Expected only 1 fetch for 10 concurrent requests")
    }

    /**
     * Demonstrates that different URLs are fetched independently.
     */
    @Test
    fun testDifferentUrlsFetchIndependently() = runTest {
        var fetchCount = 0

        suspend fun simulatedFetch(url: String): String {
            fetchCount++
            kotlinx.coroutines.delay(50)
            return "result-$url"
        }

        val cache = mutableMapOf<String, kotlinx.coroutines.Deferred<String>>()

        suspend fun getOrLoad(url: String): String {
            val deferred = synchronized(cache) {
                // Check if already in cache
                cache[url]?.let { return@synchronized it }

                // Not in cache, create deferred and start fetch
                async {
                    val result = simulatedFetch(url)
                    result
                }.also { cache[url] = it }
            }

            return deferred.await()
        }

        // Launch concurrent requests for 3 different URLs
        val requests = listOf(
            async { getOrLoad("url-1") },
            async { getOrLoad("url-2") },
            async { getOrLoad("url-3") },
        )

        val results = requests.awaitAll()

        // Verify we got 3 different results
        assertEquals(3, results.toSet().size)

        // Verify 3 fetches happened (one per unique URL)
        assertEquals(3, fetchCount)
    }
}
