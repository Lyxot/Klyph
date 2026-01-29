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
├── Parser.kt                        # Parsing helpers: unicode-range, font-weight, font-style
├── UnicodeRange.kt                  # Character matching: Unicode range parsing and matching
├── FontSliceCache.kt                # Caching: Font cache with deduplication & monitoring
└── HttpClient.kt                    # Platform: HTTP client expect/actual declaration
```

**klyph-css**

```
xyz.hyli.klyph.css/
├── CssParser.kt                     # CSS parsing: Direct CSS → FontDescriptor conversion
├── CssCache.kt                      # Caching: CSS cache with deduplication & monitoring
├── UrlUtils.kt                      # Utilities: Relative URL resolution
├── CssUrlFontDescriptorProvider.kt  # CSS URL provider implementation
├── CssContentFontDescriptorProvider.kt # CSS content provider implementation
└── HttpClient.kt                    # Platform: HTTP client expect/actual declaration
```

## Core Components

### 1. SubsetText (Primary API, klyph-core)

**File**: `SubsetText.kt`

The main user-facing composable with high-level text rendering:

- **Scoped**: `SubsetFontScope.SubsetText()` - uses the provider from scope
- **Standalone**: `SubsetText(provider = ...)` - explicit FontDescriptorProvider for one-off usage

Both variants wrap Material3 Text with automatic font subsetting powered by `rememberSubsetAnnotatedString()`.

**Key Functions:**

- `SubsetFontScope.SubsetText()`
- `SubsetText()`

### 2. SubsetAnnotatedString (Low-level API, klyph-core)

**File**: `SubsetAnnotatedString.kt`

Low-level composable for direct AnnotatedString access with font subsetting:

- **Scoped**: `SubsetFontScope.rememberSubsetAnnotatedString()`
- **Standalone**: `rememberSubsetAnnotatedString(descriptors = ...)`

Use this when you need custom AnnotatedString composition or additional styling.

**Key Functions:**

- `rememberSubsetAnnotatedString()`
- `SubsetFontScope.rememberSubsetAnnotatedString()`
- `findDescriptor()` (locality hint optimization)
- `TextInterval`
- `isWeightMatching()`, `isStyleMatching()`

### 3. SubsetFontProvider (Scoped API, klyph-core)

**File**: `SubsetFontProvider.kt`

Provides a scoped API (similar to Row/Column):

- `SubsetFontScope` holds the provider and optional fallback FontFamily
- `SubsetFontProvider` creates the scope and runs the content lambda

### 4. FontDescriptorProvider (Provider Interface, klyph-core + klyph-css)

**File**: `FontDescriptorProvider.kt` (core)

Interface for providing parsed font descriptors from various sources:

- **FontDescriptorProvider**: `suspend fun getDescriptors(): List<FontDescriptor>`
- **StaticFontDescriptorProvider** (core): static list of descriptors for bundled fonts
- **CssUrlFontDescriptorProvider** (klyph-css): loads descriptors from CSS URL via CssCache
- **CssContentFontDescriptorProvider** (klyph-css): parses CSS content via CssCache

### 5. Parser (Parsing Helpers, klyph-core)

**File**: `Parser.kt`

Shared parsing utilities used across the core module and CSS providers:

- `parseUnicodeRange()`
- `parseFontWeight()`
- `parseFontStyle()`

### 6. FontDescriptor (Font Metadata, klyph-core + klyph-css)

**File**: `FontDescriptor.kt` (core) and `CssFontDescriptor.kt` (klyph-css)

Core interface and implementations:

- **FontDescriptor**: `cacheKey`, `fontFamily`, `weight`, `style`, `unicodeRanges`, `getFontFamily()`
- **ResourceFontDescriptor** (core): loads bundled Compose resources; includes CSS-string convenience constructor
- **UrlFontDescriptor** (klyph-css): loads fonts via HTTP
- **createFontFamilyFromData()**: wraps platform Font into a FontFamily

### 7. UnicodeRange (Character Matching, klyph-core)

**File**: `UnicodeRange.kt`

- `UnicodeRange` data class
- `isCharInRanges()` helper

### 8. FontSliceCache (Font Caching, klyph-core)

**File**: `FontSliceCache.kt`

Thread-safe cache of loaded FontFamily instances with request deduplication and monitoring.

### 9. CssParser (CSS Parsing, klyph-css)

**File**: `CssParser.kt`

Parses CSS `@font-face` rules into UrlFontDescriptor instances and resolves relative URLs.

### 10. CssCache (CSS Caching, klyph-css)

**File**: `CssCache.kt`

Thread-safe cache for parsed CSS with request deduplication and bandwidth monitoring.

### 11. UrlUtils (URL Resolution, klyph-css)

**File**: `UrlUtils.kt`

Resolves relative URLs in CSS files.

### 12. HttpClient (Platform Abstraction, klyph-css)

**File**: `HttpClient.kt`

Platform-specific HTTP client declaration via expect/actual.

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
