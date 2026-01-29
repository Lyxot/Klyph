# Klyph Architecture

This document contains technical architecture details for developers working on or extending Klyph.

## File Structure

Klyph is organized into focused, single-responsibility modules. The core library lives in `klyph-core`, and CSS parsing
and caching live in `klyph-css`.

**klyph-core**
```
xyz.hyli.klyph/
├── SubsetText.kt                    # Primary API: SubsetText composable (scoped & standalone)
├── SubsetAnnotatedString.kt         # Low-level API: rememberSubsetAnnotatedString (scoped & standalone)
├── SubsetFontProvider.kt            # Scoped API: SubsetFontScope & SubsetFontProvider
├── FontDescriptorProvider.kt        # Provider interface: FontDescriptorProvider
├── FontDescriptor.kt                # Font metadata: Descriptor model and Font creation
├── UnicodeRange.kt                  # Character matching: Unicode range parsing and matching
└──  FontSliceCache.kt                # Caching: Font cache with deduplication & monitoring
```

**klyph-css**

```
xyz.hyli.klyph.css/
├── CssParser.kt                     # CSS parsing: Direct CSS → FontDescriptor conversion
├── CssCache.kt                      # Caching: CSS cache with deduplication & monitoring
├── UrlUtils.kt                      # Utilities: Relative URL resolution
├── CssUrlFontDescriptorProvider.kt  # CSS URL provider implementation
└── CssContentFontDescriptorProvider.kt # CSS content provider implementation
├── HttpClient.kt                    # Platform: HTTP client expect/actual declaration
```

## Core Components

### 1. SubsetText (Primary API)

**File**: `SubsetText.kt`

The main user-facing composable with high-level text rendering:

- **Scoped**: `SubsetFontScope.SubsetText()` - No provider parameter, gets it from scope (recommended)
- **Standalone**: `SubsetText(provider = ...)` - Explicit FontDescriptorProvider for one-off usage

Both variants wrap the standard Material3 Text composable with automatic font subsetting powered by `rememberSubsetAnnotatedString()`.

**Key Functions:**

- `SubsetFontScope.SubsetText()`: Scoped extension function that uses provider from scope
- `SubsetText()`: Standalone function with explicit provider parameter

### 2. SubsetAnnotatedString (Low-level API)

**File**: `SubsetAnnotatedString.kt`

Low-level composable for direct AnnotatedString access with font subsetting:

- **Scoped**: `SubsetFontScope.rememberSubsetAnnotatedString()` - No provider parameter, gets it from scope
- **Standalone**: `rememberSubsetAnnotatedString(descriptors = ...)` - Explicit descriptor list for direct
  AnnotatedString building

Use this when you need more control than SubsetText provides, such as building custom composables, combining multiple AnnotatedStrings, or applying additional styling.

**Key Functions:**

- `rememberSubsetAnnotatedString()`: Builds AnnotatedString with character-level font slice assignments
- `SubsetFontScope.rememberSubsetAnnotatedString()`: Scoped extension function for direct AnnotatedString access
- `findDescriptor()`: Finds descriptor for a character with locality hint optimization (O(D) to O(1))
- `TextInterval`: Data class representing a contiguous run of text with the same font
- `isWeightMatching()`, `isStyleMatching()`: Helper functions for descriptor filtering

**Algorithm Optimization:**
The character-to-font matching uses a locality hint pattern that optimizes sequential lookups from O(D) to O(1) for consecutive characters in the same script, where D is the number of font descriptors. This is particularly effective for real-world text where characters are often grouped by script (e.g., "Hello 世界 World" has 3 transitions, not 15).

### 3. SubsetFontProvider (Scoped API)

**File**: `SubsetFontProvider.kt`

Provides the scoped API pattern similar to Row/Column:

- **SubsetFontScope**: Scope class holding FontDescriptorProvider and fallback FontFamily
- **SubsetFontProvider**: Composable that creates scope and runs content lambda
- Type-safe scoped API eliminates repetitive provider parameters

**Pattern:**

```kotlin
class SubsetFontScope(
    val provider: FontDescriptorProvider,
    val fontFamily: FontFamily?
)

@Composable
fun SubsetFontProvider(
    provider: FontDescriptorProvider,
    fontFamily: FontFamily? = null,
    content: @Composable SubsetFontScope.() -> Unit
)
```

### 4. FontDescriptorProvider (Provider Interface)

**File**: `FontDescriptorProvider.kt`

Interface for providing parsed font descriptors from various sources:

- **FontDescriptorProvider**: Interface with `suspend fun getDescriptors(): List<FontDescriptor>`
- **CssUrlFontDescriptorProvider** (klyph-css): Loads descriptors from CSS URL using CssCache
- **CssContentFontDescriptorProvider** (klyph-css): Parses descriptors from CSS content string with hash-based caching
  via CssCache
- **StaticFontDescriptorProvider**: Provides a static list of pre-constructed descriptors (useful for bundled fonts)

**Design:**

- Interface abstraction allows multiple descriptor sources
- CSS providers (klyph-css) integrate with CssCache for efficient caching and request deduplication
- CssContentFontDescriptorProvider (klyph-css) uses hash-based cache keys for efficient lookups
- StaticFontDescriptorProvider useful for ResourceFontDescriptor instances loaded from Compose resources
- Extensible: custom providers can load from databases, JSON, etc.

**Pattern:**

```kotlin
interface FontDescriptorProvider {
    suspend fun getDescriptors(): List<FontDescriptor>
}

class CssUrlFontDescriptorProvider(
    private val cssUrl: String
) : FontDescriptorProvider {
    override suspend fun getDescriptors(): List<FontDescriptor> {
        return CssCache.getOrLoad(cssUrl)
    }
}

class CssContentFontDescriptorProvider(
    private val cssContent: String,
    private val baseUrl: String = ""
) : FontDescriptorProvider {
    override suspend fun getDescriptors(): List<FontDescriptor> {
        return parseCssToDescriptors(cssContent, baseUrl)
    }
}
```

### 5. CssParser (CSS Parsing, klyph-css)

**File**: `CssParser.kt` (klyph-css)

Parses CSS `@font-face` rules directly into UrlFontDescriptor objects:

- **parseCssToDescriptors()**: Main CSS parser that extracts @font-face rules and creates UrlFontDescriptor instances
- **extractUrlFromSrc()**: Extracts font URLs from CSS src descriptors (ignores local() fonts)
- **parseFontWeight()**: Converts CSS weight values to FontWeight (supports named and numeric)
- **parseFontStyle()**: Converts CSS style values to FontStyle

**Features:**

- Direct CSS → UrlFontDescriptor conversion (no intermediate data structures)
- Supports all essential CSS font descriptors (font-family, font-weight, font-style, unicode-range, src)
- Automatically resolves relative URLs against base URL
- JavaScript-compatible regex patterns (uses `[\s\S]` instead of `(?s)` flag)
- Strips CSS comments before parsing

### 6. FontDescriptor (Font Metadata)

**File**: `FontDescriptor.kt`

Defines the core font descriptor interface and implementations:

- **FontDescriptor**: Interface for font descriptors that can load and provide font data
    - Properties: `cacheKey`, `fontFamily`, `weight` (FontWeight), `style` (FontStyle), `unicodeRanges`
    - Method: `suspend fun getFont(onBytesLoaded: (Long) -> Unit): Font`
- **UrlFontDescriptor**: Data class for loading fonts from remote URLs via HTTP
    - Properties: `url`, `fontFamily`, `weight`, `style`, `unicodeRanges`
    - Cache key: URL itself
- **ResourceFontDescriptor**: Data class for loading fonts from Compose resources
    - Properties: `resource`, `fontFamily`, `weight`, `style`, `unicodeRanges`
    - Cache key: Hash of resource and metadata
- **createFontFromData()**: Creates a Compose FontFamily from ByteArray with proper metadata (Android uses ByteBuffer on
  API 29+ and a temp-file fallback below)

**Design:**

- Interface-based abstraction allows multiple font sources (URLs, resources, etc.)
- Each implementation provides its own loading logic and cache key strategy
- Typed Compose properties (not CSS strings)
- Used throughout the system as the single source of truth for font metadata

### 7. UnicodeRange (Character Matching)

**File**: `UnicodeRange.kt`

Handles unicode-range parsing and character matching:

- **UnicodeRange**: Data class representing a unicode range (start, end)
- **parseUnicodeRange()**: Parses CSS unicode-range values into UnicodeRange objects
- **isCharInRanges()**: Checks if a character falls within a list of unicode ranges

**Supported Formats:**

- Single code point: `U+26` → `UnicodeRange(0x26, 0x26)`
- Range: `U+4E00-9FFF` → `UnicodeRange(0x4E00, 0x9FFF)`
- Wildcard: `U+4??` → `UnicodeRange(0x400, 0x4FF)`
- Multiple ranges: `U+0-FF, U+131, U+152-153` → List of UnicodeRange objects

### 8. FontSliceCache (Font Caching)

**File**: `FontSliceCache.kt`

Thread-safe global cache for font files with request deduplication and monitoring:

- **Cache Storage**: `MutableMap<String, Deferred<FontFamily>>` - Stores fully created FontFamily instances
- **Thread Safety**: Mutex-protected operations
- **Request Deduplication**: Stores `Deferred` to prevent concurrent duplicate fetches
- **Descriptor Tracking**: `StateFlow<Map<String, FontDescriptor>>` - All cached font descriptors
- **Bandwidth Monitoring**: `StateFlow<Long>` - Total bytes received from font downloads
- **Operations**: `getOrLoad(descriptor)`, `preload(descriptors)`, `clear()`, `clearAsync()`

**Request Deduplication Pattern:**

```kotlin
suspend fun getOrLoad(descriptor: FontDescriptor): FontFamily = coroutineScope {
    val url = descriptor.url
    val deferred = mutex.withLock {
        cache[url]?.let { return@withLock it }
        async {
            try {
                val res = httpClient.get(url)
                val fontData = res.bodyAsBytes()
                _receivedBytes.value += res.contentLength() ?: fontData.size.toLong()
                createFontFromData(fontData, descriptor)
            } catch (e: Exception) {
                mutex.withLock { cache.remove(url) }
                throw e
            }
        }.also { cache[url] = it }
    }
    deferred.await().also {
        _descriptors.value += (url to descriptor)
    }
}
```

**Why Deduplication Matters:**
If 10 `SubsetText` instances mount simultaneously and all need the same Chinese font slice, without deduplication: 10
network requests. With deduplication: 1 network request, all share result.

### 9. CssCache (CSS Caching, klyph-css)

**File**: `CssCache.kt` (klyph-css)

Thread-safe global cache for parsed CSS files with request deduplication and monitoring:

- **Cache Storage**: `MutableMap<String, Deferred<List<FontDescriptor>>>` - Stores parsed descriptors
- **Thread Safety**: Mutex-protected operations
- **Request Deduplication**: Stores `Deferred` to prevent concurrent duplicate fetch+parse
- **Descriptor Tracking**: `StateFlow<Map<String, List<FontDescriptor>>>` - All cached CSS descriptors by URL/key
- **Bandwidth Monitoring**: `StateFlow<Long>` - Total bytes received from CSS downloads
- **Operations**:
    - `getOrLoad(url)` - Fetches and caches CSS from URL
    - `getOrParse(cssContent, baseUrl)` - Parses and caches CSS content string using hash-based key
    - `clear()`, `clearAsync()` - Clears the cache
- **Helper Function**: `getFontCssDescription()` - convenience wrapper for `CssCache.getOrLoad()`

**Hash-Based Caching for CSS Content:**
When parsing CSS content strings (not URLs), CssCache uses a hash-based key:
`"content:${cssContent.hashCode()}:${baseUrl.hashCode()}"`. This provides a short, efficient cache key without storing
the full content as the key.

**Integrated URL Resolution:**
Automatically resolves relative URLs in CSS against the CSS file's base URL during parsing.

**Key Features:**

- Direct CSS → FontDescriptor conversion (no intermediate FontFace objects)
- Supports both URL-based and content-based CSS parsing with caching
- Tracks total CSS bandwidth for monitoring
- Provides async clear for fire-and-forget cleanup

### 10. UrlUtils (URL Resolution, klyph-css)

**File**: `UrlUtils.kt` (klyph-css)

Resolves relative URLs in CSS files:

- **resolveUrl()**: Main resolution function that handles all URL types
- **Supports**: Absolute URLs, relative paths (`./`, `../`), absolute paths (`/`), protocol-relative (`//`), data URLs

**Example:**

```kotlin
resolveUrl("https://example.com/css/fonts.css", "./font.woff2")
// → "https://example.com/css/font.woff2"

resolveUrl("https://example.com/css/fonts.css", "../assets/font.woff2")
// → "https://example.com/assets/font.woff2"
```

### 11. HttpClient (Platform Abstraction)

**File**: `HttpClient.kt`

Platform-specific HTTP client declaration using Kotlin's expect/actual pattern:

```kotlin
expect val httpClient: HttpClient
```

Implementations provided in platform-specific source sets (androidMain, iosMain, jvmMain, jsMain, wasmJsMain).

## Data Flow

```
1. User calls SubsetText("Hello 世界!")
        ↓
2. CSS Fetching & Caching
   - CssCache.getOrLoad(cssUrl) (klyph-css) checks cache
   - If miss: httpClient fetches CSS
   - parseCssToObjects() parses @font-face rules
   - resolveUrl() resolves relative URLs
   - Result cached in CssCache (klyph-css)
        ↓
3. Descriptor Parsing & Filtering
   - parseFontDescriptor() converts FontFace to FontDescriptor
   - Filter by requested weight/style
        ↓
4. Character Analysis & Interval Building
   - Analyze text character by character
   - findDescriptor() matches each char to descriptor using unicode-range
   - Group consecutive chars with same font into TextIntervals
   - Optimization: locality hint reduces lookups from O(D) to O(1)
        ↓
5. Font Loading & Caching
   - For each unique descriptor in intervals:
     - FontSliceCache.getOrLoad(descriptor) checks cache
     - If miss: httpClient fetches font data
     - createFontFromData() creates Compose Font
     - Wrap in FontFamily(singleFont)
     - Result cached in FontSliceCache (FontFamily)
        ↓
6. AnnotatedString Building
   - For each TextInterval:
     - Get substring
     - Get FontFamily from descriptor map
     - Apply SpanStyle(fontFamily = ...) to span
   - Fallback to default for unmapped chars
        ↓
7. Rendering
   - Standard Text() composable renders the AnnotatedString
   - Compose applies correct font to each span
```

## Design Decisions

### Why Separate Files for Each Component?

The reorganized structure provides:

- **Single Responsibility**: Each file has one clear purpose
- **Easy Navigation**: Developers can quickly find relevant code
- **Better Testability**: Components can be tested in isolation
- **Maintainability**: Changes to one component don't affect others
- **Clear Dependencies**: Import statements make dependencies explicit

### Why AnnotatedString?

Alternative considered: Combine all fonts into one `FontFamily(latinFont, chineseFont, japaneseFont)`

Problem: Compose's FontFamily can only select one font at render time, causing characters from other fonts to appear
blank.

Solution: Use AnnotatedString with per-character FontFamily assignments. Each FontFamily contains only one font slice,
ensuring correct selection.

### Why Two Separate Caches (FontSliceCache and CssCache)?

Separation of concerns:

- **FontSliceCache**: Caches FontFamily instances created from font data (50-200 KB per slice)
- **CssCache** (klyph-css): Caches parsed CSS metadata (List<FontFace>), lightweight structured data
- **Different Lifetimes**: CSS rarely changes, fonts loaded on-demand
- **Clear Responsibilities**: Font loading vs CSS parsing are distinct operations
- **Independent Monitoring**: Separate size StateFlows for debugging

### Why Global Cache with Request Deduplication?

Font and CSS data are expensive to load (network + parsing). Without request deduplication:

- 10 `SubsetText` instances mounting simultaneously = 10 identical network requests
- Race condition: all see cache empty, all fetch the same resource

Global cache with request deduplication ensures:

- Fonts and CSS loaded only once per session
- Concurrent requests for same URL result in single network fetch
- Multiple SubsetText instances share loaded resources
- Memory reused efficiently
- Thread-safe via Mutex + Deferred pattern

Implementation: Uses `Deferred<T>` stored in cache so first request starts fetch, subsequent requests await the same
`Deferred`.

### Why Character-by-Character?

Character-level approach provides:

- **Maximum Precision**: Each character gets exact font needed
- **Simplicity**: No word boundary detection needed
- **Robustness**: Works for all languages including CJK
- **Negligible Performance Impact**: Consecutive chars batched into TextIntervals
- **Locality Optimization**: Sequential lookups optimized with hint pattern

## Performance Characteristics

### Network Requests:

- CSS: 1 request per CSS URL (cached globally with deduplication)
- Font slices: 1 request per unique font URL (cached globally with deduplication)
- Example: "Hello 世界" with shared CSS = 1 CSS fetch + 2 font slice fetches (first render)
- Subsequent renders: 0 network requests (all cached)

### Memory Usage:

- FontSliceCache: Holds FontFamily instances for each loaded font slice
- Typical slice: 50-200 KB
- Example session with 10 slices: ~1-2 MB total
- CssCache (klyph-css): Holds parsed FontDescriptor lists (lightweight metadata)
- Typical CSS: <10 KB in memory
- Both caches track descriptors and bandwidth usage via StateFlows

### Rendering Performance:

- Text analysis & interval building: O(n) where n = text length
- Character matching: Amortized O(n) with locality hint (O(n×d) worst case, where d = descriptor count)
- AnnotatedString building: O(i) where i = number of intervals
- Negligible impact vs standard Text (batching minimizes spans)

### Cache Efficiency:

- Request deduplication prevents duplicate work
- Global session cache maximizes hit rate
- StateFlow size monitoring enables reactive debugging

## Known Limitations

1. **Platform Support**: Android/iOS/JVM are supported, but Android ByteArray fonts use a temp-file fallback on API < 29
   due to platform limitations.
2. **Font Formats**: Depends on platform support (WOFF2 on web)
3. **Complex Scripts**: No special handling for ligatures or kerning across font boundaries
4. **Dynamic CSS**: No support for CSS variables, `@import` rules (planned), or nested @font-face rules
5. **Local Fonts**: `local()` in src is ignored (web-only limitation)
6. **Font Fallback**: Limited fallback support - italic automatically falls back to normal style, but unmapped
   characters use system default. Configurable fallback chains not yet supported

## Future Enhancements

### Potential Features:

1. Automatic CSS generation tool with unicode-range from font files
2. Font subsetting tool to create subsets based on app text analysis
3. Intelligent preload optimization by analyzing app text patterns
4. Expand native platform support with platform-specific optimizations (iOS, Android, Desktop)
5. Advanced typography: ligatures, kerning, OpenType features
6. Progressive font loading for large character sets with priority-based loading
7. Font variant support (small-caps, numeric variants, etc.)
8. Configurable fallback chains for unmapped characters

### API Evolution:

- SubsetText as primary API (scoped + standalone versions)
- SubsetTextField for text input with dynamic font loading
- SubsetButton, SubsetLabel for Material3 integration
- More scoped composables for common text components
- Font preloading hints and analytics
