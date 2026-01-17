# Klyph

**Intelligent Font Subsetting for Compose Multiplatform**

Klyph is a Kotlin Multiplatform library that brings web-style font subsetting to Compose applications. It dramatically reduces font loading overhead by fetching only the specific character slices needed for your text, making it ideal for applications using large font files like Chinese, Japanese, or Korean typefaces.

## Features

- **Smart Font Subsetting**: Automatically loads only the font slices needed for the characters you're rendering
- **CSS Integration**: Parses standard CSS `@font-face` rules with `unicode-range` descriptors
- **Automatic URL Resolution**: Handles relative URLs in CSS files (e.g., `./font.woff2`, `../fonts/font.woff2`)
- **Global Caching**: Font slices and CSS files are cached across the entire application session with request
  deduplication
- **Lazy Loading**: Font data is loaded on-demand as characters are rendered
- **Compose-First**: Seamlessly integrates with Compose Multiplatform using familiar scoped APIs
- **Multiplatform Support**: Works on JavaScript and WebAssembly targets

## Why Klyph?

Traditional font loading in Compose requires downloading entire font files, which can be problematic for:
- **CJK Fonts**: Chinese/Japanese/Korean fonts can be 10+ MB per weight
- **Network Efficiency**: Loading fonts you don't need wastes bandwidth
- **Performance**: Large font downloads slow down app startup

Klyph solves this by:
1. Parsing CSS files that define font slices with unicode-range
2. Analyzing your text to determine which characters are needed
3. Loading only the relevant font slices from the cache or network
4. Applying them character-by-character using AnnotatedString

## Installation

### Publishing to MavenLocal

For now, publish to MavenLocal to use in your projects:

```bash
./gradlew :klyph:publishToMavenLocal
```

Then add the dependency to your Compose Multiplatform project:

```kotlin
commonMain.dependencies {
    implementation("xyz.hyli:klyph:1.0.0")
}
```

## Quick Start

### Basic Usage with SubsetText

Use `SubsetText` within a `SubsetFontProvider` scope for automatic font subsetting:

```kotlin
import xyz.hyli.klyph.SubsetFontProvider
import xyz.hyli.klyph.SubsetText

@Composable
fun MyApp() {
    SubsetFontProvider(cssUrl = "https://fonts.googleapis.com/css2?family=Noto+Sans+SC") {
        // SubsetText automatically gets cssUrl from the scope - no need to pass it!
        SubsetText(
            text = "你好世界 Hello World",
            fontSize = 20.sp
        )

        // Multiple SubsetText calls share the same CSS URL
        SubsetText(
            text = "Another text 另一段文字",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
```

**Key Benefits of Scoped API:**
- `SubsetText` is a scoped function within `SubsetFontProvider` - no cssUrl parameter needed!
- Type-safe: SubsetText only works within the provider scope
- Clean API: Similar to how `Row`, `Column` provide scoped modifiers

`SubsetText` is a drop-in replacement for Material3's `Text` with the same API. It automatically:
- Analyzes each character in your text
- Loads only the necessary font slices
- Applies the correct font to each character using `AnnotatedString`

### Direct CSS URL Usage (Without Provider)

You can also specify the CSS URL directly for one-off usage:

```kotlin
@Composable
fun MyComponent() {
    SubsetText(
        text = "你好世界 Direct CSS loading",
        cssUrl = "https://example.com/fonts.css",
        fontSize = 16.sp
    )
}
```

### Font Weight and Style

`SubsetText` supports all standard Text parameters:

```kotlin
@Composable
fun StyledText() {
    SubsetFontProvider(cssUrl = "https://example.com/fonts.css") {
        SubsetText(
            text = "粗体文字 Bold Text",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Blue,
            textDecoration = TextDecoration.Underline
        )
    }
}
```

## How It Works

### The Challenge: Compose FontFamily Limitation

Compose's `FontFamily` can only select **one** font from a provided list at a time. When you have multiple font slices (e.g., one for Latin characters, one for Chinese characters), Compose will only use one of them, leaving characters from other slices blank.

### Klyph's Solution: Scoped API + Character-Level Font Application

Klyph uses a scoped API pattern (similar to `Row`/`Column`) combined with character-level font application:

**Scoped API Benefits:**
- `SubsetFontProvider` creates a `SubsetFontScope`
- `SubsetText` is an extension function on the scope
- CSS URL is captured once and reused by all SubsetText calls
- Type-safe: SubsetText only available within the provider
- Clean, ergonomic API without parameter repetition

**Character-Level Font Application:**
Klyph's `SubsetText` solves the FontFamily limitation by applying fonts character-by-character:

1. **CSS Parsing**: Fetches and parses CSS files containing `@font-face` rules with `unicode-range` descriptors
2. **URL Resolution**: Relative URLs in CSS (like `url(./font.woff2)`) are automatically resolved against the CSS file's base URL
3. **Character Analysis**: Analyzes each character in your text and groups consecutive characters using the same font
4. **Range Matching**: Each character is matched against unicode-range values to determine which font slice it needs
5. **Lazy Loading**: Only the matched font slices are fetched from the network (or cache)
6. **AnnotatedString Building**: Builds an `AnnotatedString` where each character span gets its own `FontFamily` (containing only one font slice)
7. **Rendering**: The standard Text composable renders the AnnotatedString with proper font assignments
8. **Global Caching**: Once a font slice or CSS file is loaded, it's cached globally for the session with request
   deduplication

This approach ensures that "Hello 你好" correctly renders "Hello" with the Latin font slice and "你好" with the Chinese font slice, all in the same text element.

### Unicode Range Support

Klyph supports all standard CSS unicode-range formats:
- Single code point: `U+26`
- Range: `U+4E00-9FFF`
- Wildcard: `U+4??` (equivalent to `U+400-4FF`)
- Multiple ranges: `U+0-FF, U+131, U+152-153`

## API Reference

### Core Components

#### `SubsetFontProvider` (Scope Provider)

```kotlin
@Composable
fun SubsetFontProvider(
    cssUrl: String,
    fontFamily: FontFamily? = null,
    content: @Composable SubsetFontScope.() -> Unit
)
```

Provides a CSS URL scope for font subsetting. Creates a `SubsetFontScope` where the scoped `SubsetText` function is available.

**Parameters:**

- `cssUrl`: The URL of the CSS file containing @font-face rules
- `fontFamily`: Optional fallback FontFamily for characters not covered by subset fonts
- `content`: Composable content within the SubsetFontScope

**Usage:**
```kotlin
SubsetFontProvider(cssUrl = "https://example.com/fonts.css") {
    // Within this scope, SubsetText doesn't need cssUrl parameter
    SubsetText(text = "Hello 世界", fontSize = 20.sp)
}
```

#### `SubsetFontScope.SubsetText` (Primary API - Scoped)

```kotlin
@Composable
fun SubsetFontScope.SubsetText(
    text: String,
    modifier: Modifier = Modifier,
    // ... all standard Text parameters ...
    style: TextStyle = LocalTextStyle.current
): Unit
```

A scoped function available within `SubsetFontProvider` that automatically loads and applies font slices character-by-character. This is the **recommended way** to use Klyph.

**Features:**
- Scoped to `SubsetFontProvider` - automatically gets CSS URL from scope
- Drop-in replacement for Material3's `Text` with identical API
- Automatically matches characters to font slices via unicode-range
- Builds AnnotatedString with per-character font assignments
- Supports all Text parameters (color, fontSize, fontWeight, etc.)
- Type-safe: Only available within provider scope

#### `SubsetText` (Standalone - With cssUrl)

```kotlin
@Composable
fun SubsetText(
    text: String,
    // ... all standard Text parameters ...
    cssUrl: String
): Unit
```

Standalone version that requires explicit CSS URL. Use this for one-off cases outside of `SubsetFontProvider`.

### Utility Functions and Caches

#### `getFontCssDescription`

```kotlin
suspend fun getFontCssDescription(url: String): List<FontFace>
```

Fetches and parses CSS to extract `@font-face` rules. Automatically uses `CssCache` internally for caching and request
deduplication.

#### `FontSliceCache`

Global cache for loaded font slices with request deduplication:

```kotlin
object FontSliceCache {
   val size: StateFlow<Int>                       // number of cached slices
   suspend fun getOrLoad(url: String): ByteArray  // Gets from cache or fetches
   suspend fun preload(urls: List<String>)        // Preloads multiple fonts
   suspend fun clear()                            // Clears all cached fonts
}
```

**Request Deduplication**: When multiple concurrent requests are made for the same font URL, only one network request is
performed and all requests share the result.

**Example:**
If 10 `SubsetText` instances all render "你好" and mount simultaneously, they will all request the same Chinese font
slice. Without deduplication, this would trigger 10 identical network requests. With deduplication, only 1 request is
made and all 10 instances share the result.

#### `CssCache`

Global cache for parsed CSS files with request deduplication:

```kotlin
object CssCache {
   val size: StateFlow<Int>                            // number of cached CSS files
   suspend fun getOrLoad(url: String): List<FontFace>  // Gets from cache or fetches+parses
   suspend fun clear()                                 // Clears all cached CSS
}
```

**Request Deduplication**: When multiple concurrent requests are made for the same CSS URL, only one network request and
parse operation is performed.

## Example CSS Format

Klyph works with CSS files that define font slices using unicode-range. Both absolute and relative URLs are supported:

```css
/* Absolute URLs */
@font-face {
  font-family: 'Noto Sans SC';
  font-weight: 400;
  src: url(https://example.com/font-latin.woff2) format('woff2');
  unicode-range: U+0000-00FF, U+0131, U+0152-0153;
}

/* Relative URLs (automatically resolved against CSS file URL) */
@font-face {
  font-family: 'Noto Sans SC';
  font-weight: 400;
  src: url(./fonts/font-chinese.woff2) format('woff2');
  unicode-range: U+4E00-9FFF;
}

/* Parent directory relative URLs */
@font-face {
  font-family: 'Noto Sans SC';
  font-weight: 400;
  src: url(../assets/font-japanese.woff2) format('woff2');
  unicode-range: U+3040-309F;
}
```

When you render "Hello 你好", Klyph will:
1. Load `font-latin.woff2` for "Hello " (U+0048, U+0065, U+006C, U+006F, U+0020)
2. Load `fonts/font-chinese.woff2` (resolved from `./fonts/font-chinese.woff2`) for "你好" (U+4F60, U+597D)
3. Skip any other font slices

## Platform Support

Currently supports:
- ✅ JavaScript (Browser)
- ✅ WebAssembly JavaScript (Browser)

## Performance Tips

1. **Reuse Text**: If rendering the same text multiple times, `SubsetText` will only compute once per unique text content
2. **Cache Warming**: Preload font slices for common characters:
   ```kotlin
   LaunchedEffect(Unit) {
       FontSliceCache.preload(listOf(
           "https://example.com/font-common-chars.woff2"
       ))
   }
   ```
3. **Static Text**: For static text that doesn't change, font slices are loaded once and cached
4. **Dynamic Text**: The system efficiently handles dynamic text by only loading new slices as needed
5. **Scoped Provider**: Use `SubsetFontProvider` to wrap multiple `SubsetText` instances that share the same CSS URL for
   better performance

## Comparison: Traditional vs. Klyph

### Traditional Approach
```kotlin
// Loads ENTIRE 12 MB font file for 2 characters!
val fontFamily = FontFamily(Font(url = "large-chinese-font.woff2"))
Text("你好", fontFamily = fontFamily)

// For mixed text, you're limited to one font:
Text("Hello 你好", fontFamily = chineseFont) // "Hello" uses Chinese font (wrong)
Text("Hello 你好", fontFamily = latinFont)   // "你好" renders blank (broken)
```

### Klyph Approach
```kotlin
// Loads only ~50 KB slice containing "你好"
SubsetFontProvider(cssUrl = "font-subset.css") {
    SubsetText("你好") // No cssUrl parameter - automatically from scope!
}

// Mixed text works perfectly - each character gets the right font:
SubsetFontProvider(cssUrl = "font-subset.css") {
    SubsetText("Hello 你好") // Latin slice for "Hello", Chinese slice for "你好"
    SubsetText("More text 更多文字") // Reuses already-loaded slices
}
```

**Benefits:**
- 99.6% reduction in font data transferred
- Perfect rendering of mixed-language text
- Character-level font precision
- Clean scoped API - no repetitive cssUrl parameters

## Architecture

### File Structure

Klyph is organized into focused, single-responsibility modules:

```
xyz.hyli.klyph/
├── SubsetText.kt           # Primary API: SubsetText composable (scoped & standalone)
├── SubsetFontProvider.kt   # Scoped API: SubsetFontScope & SubsetFontProvider
├── FontFace.kt             # CSS parsing: @font-face rule parsing
├── FontDescriptor.kt       # Font metadata: Descriptor model and parsing
├── UnicodeRange.kt         # Character matching: Unicode range parsing and matching
├── FontSliceCache.kt       # Caching: Font file cache with request deduplication
├── CssCache.kt             # Caching: CSS file cache with request deduplication
├── UrlUtils.kt             # Utilities: Relative URL resolution
└── HttpClient.kt           # Platform: HTTP client expect/actual declaration
```

### Core Components

#### 1. SubsetText (Primary API)
**File**: `SubsetText.kt`

The main user-facing composable with two variants:
- **Scoped**: `SubsetFontScope.SubsetText()` - No cssUrl parameter, gets it from scope
- **Standalone**: `SubsetText(cssUrl = ...)` - Explicit CSS URL for one-off usage

**Key Functions:**
- `rememberSubsetAnnotatedString()`: Builds AnnotatedString with font assignments
- `findDescriptor()`: Finds descriptor for a character with locality hint optimization
- `TextInterval`: Data class representing a contiguous run of text with the same font

**Algorithm Optimization:**
The character-to-font matching uses a locality hint pattern that optimizes sequential lookups from O(D) to O(1) for
consecutive characters in the same script, where D is the number of font descriptors. This is particularly effective for
real-world text where characters are often grouped by script (e.g., "Hello 世界 World" has 3 transitions, not 15).

#### 2. SubsetFontProvider (Scoped API)

**File**: `SubsetFontProvider.kt`

Provides the scoped API pattern similar to Row/Column:

- **SubsetFontScope**: Scope class holding CSS URL and fallback FontFamily
- **SubsetFontProvider**: Composable that creates scope and runs content lambda
- Type-safe scoped API eliminates repetitive cssUrl parameters

**Pattern:**

```kotlin
class SubsetFontScope internal constructor(
   internal val cssUrl: String,
   internal val fontFamily: FontFamily?
)

@Composable
fun SubsetFontProvider(
   cssUrl: String,
   fontFamily: FontFamily? = null,
   content: @Composable SubsetFontScope.() -> Unit
)
```

#### 3. FontFace (CSS Parsing)
**File**: `FontFace.kt`

Parses CSS `@font-face` rules into structured data:

- **FontFace**: Complete @font-face rule representation with font-family, weight, style, unicode-range, src
- **FontSrc**: Single src descriptor (url, local, format, tech)
- **parseCssToObjects()**: Main CSS parser that extracts @font-face rules and resolves relative URLs

**Features:**

- Supports all CSS font descriptors (font-family, font-weight, font-style, unicode-range, src)
- Handles multiple src formats (url, local, format, tech)
- Automatically resolves relative URLs against base URL

#### 4. FontDescriptor (Font Metadata)

**File**: `FontDescriptor.kt`

Handles font descriptor parsing and font creation:

- **ParsedFontDescriptor**: Structured font descriptor with typed properties (url, fontFamily, weight, style,
  unicodeRanges)
- **parseFontDescriptor()**: Converts FontFace to ParsedFontDescriptor with typed properties
- **createFontFromData()**: Creates Compose Font from ByteArray with proper metadata
- **Helper Functions**: `parseFontWeight()`, `parseFontStyle()` for CSS value parsing

**Type Conversion:**
Converts from CSS string values to Compose types:

- `"normal"`, `"bold"`, `"400"` → `FontWeight`
- `"normal"`, `"italic"`, `"oblique"` → `FontStyle`

#### 5. UnicodeRange (Character Matching)
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

#### 6. FontSliceCache (Font Caching)
**File**: `FontSliceCache.kt`

Thread-safe global cache for font files with request deduplication:

- **Cache Storage**: `MutableMap<String, Deferred<ByteArray>>`
- **Thread Safety**: Mutex-protected operations
- **Request Deduplication**: Stores `Deferred` to prevent concurrent duplicate fetches
- **Size Tracking**: `StateFlow<Int>` for reactive cache size monitoring
- **Operations**: `getOrLoad()`, `preload()`, `clear()`

**Request Deduplication Pattern:**
```kotlin
val deferred = mutex.withLock {
   cache[url]?.let { return@withLock it }
   async {
      try {
         httpClient.get(url).body<ByteArray>()
      } catch (e: Exception) {
         mutex.withLock { cache.remove(url) }
         throw e
      }
   }.also { cache[url] = it }
}
return deferred.await()
```

**Why Deduplication Matters:**
If 10 `SubsetText` instances mount simultaneously and all need the same Chinese font slice, without deduplication: 10
network requests. With deduplication: 1 network request, all share result.

#### 7. CssCache (CSS Caching)

**File**: `CssCache.kt`

Thread-safe global cache for parsed CSS files with request deduplication:

- **Cache Storage**: `MutableMap<String, Deferred<List<FontFace>>>`
- **Thread Safety**: Mutex-protected operations
- **Request Deduplication**: Stores `Deferred` to prevent concurrent duplicate fetch+parse
- **Size Tracking**: `StateFlow<Int>` for reactive cache size monitoring
- **Operations**: `getOrLoad()`, `clear()`
- **Helper Function**: `getFontCssDescription()` - convenience wrapper for `CssCache.getOrLoad()`

**Integrated URL Resolution:**
Automatically resolves relative URLs in CSS against the CSS file's base URL during parsing.

#### 8. UrlUtils (URL Resolution)
**File**: `UrlUtils.kt`

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

#### 9. HttpClient (Platform Abstraction)

**File**: `HttpClient.kt`

Platform-specific HTTP client declaration using Kotlin's expect/actual pattern:

```kotlin
expect val httpClient: HttpClient
```

Implementations provided in platform-specific source sets (jsMain, wasmJsMain).

### Data Flow

```
1. User calls SubsetText("Hello 世界!")
        ↓
2. CSS Fetching & Caching
   - CssCache.getOrLoad(cssUrl) checks cache
   - If miss: httpClient fetches CSS
   - parseCssToObjects() parses @font-face rules
   - resolveUrl() resolves relative URLs
   - Result cached in CssCache
        ↓
3. Descriptor Parsing & Filtering
   - parseFontDescriptor() converts FontFace to ParsedFontDescriptor
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
     - FontSliceCache.getOrLoad(url) checks cache
     - If miss: httpClient fetches font data
     - createFontFromData() creates Compose Font
     - Wrap in FontFamily(singleFont)
     - Result cached in FontSliceCache
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

### Design Decisions

**Why Separate Files for Each Component?**

The reorganized structure provides:

- **Single Responsibility**: Each file has one clear purpose
- **Easy Navigation**: Developers can quickly find relevant code
- **Better Testability**: Components can be tested in isolation
- **Maintainability**: Changes to one component don't affect others
- **Clear Dependencies**: Import statements make dependencies explicit

**Why AnnotatedString?**

Alternative considered: Combine all fonts into one `FontFamily(latinFont, chineseFont, japaneseFont)`

Problem: Compose's FontFamily can only select one font at render time, causing characters from other fonts to appear blank.

Solution: Use AnnotatedString with per-character FontFamily assignments. Each FontFamily contains only one font slice, ensuring correct selection.

**Why Two Separate Caches (FontSliceCache and CssCache)?**

Separation of concerns:

- **FontSliceCache**: Caches binary font data (ByteArray), potentially large (50-200 KB each)
- **CssCache**: Caches parsed CSS metadata (List<FontFace>), lightweight structured data
- **Different Lifetimes**: CSS rarely changes, fonts loaded on-demand
- **Clear Responsibilities**: Font loading vs CSS parsing are distinct operations
- **Independent Monitoring**: Separate size StateFlows for debugging

**Why Global Cache with Request Deduplication?**

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

**Why Character-by-Character?**

Character-level approach provides:

- **Maximum Precision**: Each character gets exact font needed
- **Simplicity**: No word boundary detection needed
- **Robustness**: Works for all languages including CJK
- **Negligible Performance Impact**: Consecutive chars batched into TextIntervals
- **Locality Optimization**: Sequential lookups optimized with hint pattern

### Performance Characteristics

**Network Requests:**

- CSS: 1 request per CSS URL (cached globally with deduplication)
- Font slices: 1 request per unique font URL (cached globally with deduplication)
- Example: "Hello 世界" with shared CSS = 1 CSS fetch + 2 font slice fetches (first render)
- Subsequent renders: 0 network requests (all cached)

**Memory Usage:**

- FontSliceCache: Holds ByteArray for each loaded font slice
- Typical slice: 50-200 KB
- Example session with 10 slices: ~1-2 MB total
- CssCache: Holds parsed FontFace lists (lightweight metadata)
- Typical CSS: <10 KB in memory

**Rendering Performance:**

- Text analysis & interval building: O(n) where n = text length
- Character matching: Amortized O(n) with locality hint (O(n×d) worst case, where d = descriptor count)
- AnnotatedString building: O(i) where i = number of intervals
- Negligible impact vs standard Text (batching minimizes spans)

**Cache Efficiency:**

- Request deduplication prevents duplicate work
- Global session cache maximizes hit rate
- StateFlow size monitoring enables reactive debugging

### Known Limitations

1. **Platform Support**: Currently JS/Wasm only (native platforms would need platform-specific HttpClient and Font
   loading implementations)
2. **Font Formats**: Depends on platform support (WOFF2 on web)
3. **Complex Scripts**: No special handling for ligatures or kerning across font boundaries
4. **Dynamic CSS**: No support for CSS variables, @import, or nested @font-face rules
5. **Local Fonts**: `local()` in src is ignored (web-only limitation)
6. **Font Fallback**: Unmapped characters use system default, not configurable fallback chains

### Future Enhancements

**Potential Features:**

1. Automatic CSS generation tool with unicode-range from font files
2. Font subsetting tool to create subsets based on app text analysis
3. Intelligent preload optimization by analyzing app text patterns
4. Native platform support (iOS, Android, Desktop) with platform-specific implementations
5. Advanced typography: ligatures, kerning, OpenType features
6. Progressive font loading for large character sets with priority-based loading
7. Font variant support (small-caps, numeric variants, etc.)
8. Configurable fallback chains for unmapped characters

**API Evolution:**

- SubsetText as primary API (scoped + standalone versions)
- SubsetTextField for text input with dynamic font loading
- SubsetButton, SubsetLabel for Material3 integration
- More scoped composables for common text components
- Font preloading hints and analytics

## Run Sample App

Run the sample app to see Klyph in action:

- JavaScript: `./gradlew :sample:composeApp:jsBrowserRun`
- Wasm: `./gradlew :sample:composeApp:wasmJsBrowserRun`

The sample demonstrates:

- Basic font subsetting with Chinese/Latin mixed text
- Multiple font weights and styles
- Large text content with performance benefits
- Real-time cache monitoring (font slice cache and CSS cache sizes)

## Publishing

### Publish to MavenLocal

1) Run `./gradlew :klyph:publishToMavenLocal`
2) Open `~/.m2/repository/xyz/hyli/`

### Publish to MavenCentral

1) Create an account and a namespace on Sonatype:
   https://central.sonatype.org/register/central-portal/#create-an-account
2) Add developer id, name, email and the project url to
   `./klyph/build.gradle.kts`
3) Generate a GPG key:
   https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/#generating-a-gpg-key-pair
   ```
   gpg --full-gen-key
   gpg --keyserver keyserver.ubuntu.com --send-keys XXXXXXXX
   gpg --export-secret-key XXXXXXXX > XXXXXXXX.gpg
   ```
4) Add these lines to `gradle.properties`:
   ```
   signing.keyId=XXXXXXXX
   signing.password=[key password]
   signing.secretKeyRingFile=../XXXXXXXX.gpg
   mavenCentralUsername=[generated username]
   mavenCentralPassword=[generated password]
   ```
5) Run `./gradlew :klyph:publishAndReleaseToMavenCentral --no-configuration-cache`

## License

```
Copyright 2026 Klyph Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

See [LICENSE](LICENSE) for the full license text.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
