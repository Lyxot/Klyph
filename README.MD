# Klyph

**Intelligent Font Subsetting for Compose Multiplatform**

Klyph is a Kotlin Multiplatform library that brings web-style font subsetting to Compose applications. It dramatically reduces font loading overhead by fetching only the specific character slices needed for your text, making it ideal for applications using large font files like Chinese, Japanese, or Korean typefaces.

## Features

- **Smart Font Subsetting**: Automatically loads only the font slices needed for the characters you're rendering
- **CSS Integration**: Parses standard CSS `@font-face` rules with `unicode-range` descriptors
- **Automatic URL Resolution**: Handles relative URLs in CSS files (e.g., `./font.woff2`, `../fonts/font.woff2`)
- **Global Caching**: Font slices are cached across the entire application session to minimize network requests
- **Lazy Loading**: Font data is loaded on-demand as characters are rendered
- **Compose-First**: Seamlessly integrates with Compose Multiplatform using familiar APIs
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
4. Composing them into a standard Compose `FontFamily`

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
3. **Character Analysis**: Analyzes each unique character in your text
4. **Range Matching**: Each character is matched against unicode-range values to determine which font slice it needs
5. **Lazy Loading**: Only the matched font slices are fetched from the network (or cache)
6. **AnnotatedString Building**: Builds an `AnnotatedString` where each character span gets its own `FontFamily` (containing only one font slice)
7. **Rendering**: The standard Text composable renders the AnnotatedString with proper font assignments
8. **Global Caching**: Once a font slice is loaded, it's cached globally for the session

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
    content: @Composable SubsetFontScope.() -> Unit
)
```

Provides a CSS URL scope for font subsetting. Creates a `SubsetFontScope` where the scoped `SubsetText` function is available.

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

### Utility Functions

#### `getFontCssDescription`

```kotlin
suspend fun getFontCssDescription(url: String): List<FontFace>
```

Fetches and parses CSS to extract `@font-face` rules.

#### `FontSliceCache`

Global cache for loaded font slices:

```kotlin
object FontSliceCache {
    suspend fun getOrLoad(url: String): ByteArray
    suspend fun preload(urls: List<String>)
    suspend fun clear()
    suspend fun size(): Int
}
```

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

### Core Components

Klyph is built from several focused components that work together to provide efficient font subsetting:

#### 1. SubsetText (Primary API)
**File**: `SubsetText.kt`

The main user-facing composable with two variants:
- **Scoped**: `SubsetFontScope.SubsetText()` - No cssUrl parameter, gets it from scope
- **Standalone**: `SubsetText(cssUrl = ...)` - Explicit CSS URL for one-off usage

Key functions:
- `rememberSubsetAnnotatedString()`: Builds AnnotatedString with font assignments
- `buildCharToDescriptorMap()`: Maps characters to font descriptors

#### 2. FontFace (CSS Parsing)
**File**: `FontFace.kt`

Parses CSS `@font-face` rules into structured data:
- `FontFace`: Complete @font-face rule representation
- `FontSrc`: Single src descriptor (url, local, format, tech)
- `parseCssToObjects()`: Parses CSS string into FontFace objects
- Supports all CSS font descriptors and resolves relative URLs

#### 3. UnicodeRange (Character Matching)
**File**: `UnicodeRange.kt`

Handles unicode-range parsing and character matching:
- Supports: Single code points (`U+26`), ranges (`U+4E00-9FFF`), wildcards (`U+4??`), multiple ranges
- `parseUnicodeRange()`: Parses CSS unicode-range values
- `isCharInRanges()`: Checks if character falls within ranges

#### 4. FontSliceCache (Global Caching)
**File**: `FontSliceCache.kt`

Thread-safe global cache using Mutex:
- `getOrLoad()`: Gets from cache or loads from network
- `preload()`: Preloads multiple fonts
- `clear()`: Clears cache
- Helper functions for parsing font descriptors and creating Compose fonts

#### 5. UrlUtils (URL Resolution)
**File**: `UrlUtils.kt`

Resolves relative URLs in CSS:
- Handles absolute URLs, relative paths (`./`, `../`), absolute paths (`/`), protocol-relative (`//`), and data URLs
- `resolveUrl()`: Main resolution function

#### 6. FontFamily (Scope & Utilities)
**File**: `FontFamily.kt`

Provides scoped API pattern:
- `SubsetFontScope`: Scope class holding CSS URL
- `SubsetFontProvider`: Creates scope and runs content lambda
- `getFontCssDescription()`: Fetches and parses CSS
- Type-safe scoped API similar to `RowScope`, `ColumnScope`

### Data Flow

```
1. User calls SubsetText("Hello 世界!")
        ↓
2. CSS Fetching & Parsing
   - getFontCssDescription() fetches CSS
   - parseCssToObjects() parses @font-face rules
   - resolveUrl() resolves relative URLs
        ↓
3. Character Analysis
   - buildCharToDescriptorMap() analyzes each character
   - Match against unicode-range values
   - Returns Map<Char, ParsedFontDescriptor>
        ↓
4. Font Loading
   - FontSliceCache.getOrLoad() for each unique descriptor
   - createFontFromData() creates Compose Font
   - Wrap in FontFamily(singleFont)
        ↓
5. AnnotatedString Building
   - Group consecutive chars with same font
   - Apply SpanStyle(fontFamily = ...) to each group
   - Fallback to FontFamily.Default for unmapped chars
        ↓
6. Rendering
   - Standard Text() renders the AnnotatedString
   - Compose applies correct font to each span
```

### Design Decisions

**Why AnnotatedString?**

Alternative considered: Combine all fonts into one `FontFamily(latinFont, chineseFont, japaneseFont)`

Problem: Compose's FontFamily can only select one font at render time, causing characters from other fonts to appear blank.

Solution: Use AnnotatedString with per-character FontFamily assignments. Each FontFamily contains only one font slice, ensuring correct selection.

**Why Global Cache?**

Font data is expensive to load (network + parsing). Global cache ensures:
- Fonts loaded only once per session
- Multiple SubsetText instances share loaded fonts
- Memory reused efficiently
- Thread-safe via Mutex

**Why Character-by-Character?**

Character-level approach provides:
- Maximum precision (each character gets exact font)
- Simplicity (no word boundary detection)
- Robustness (works for all languages including CJK)
- Negligible performance impact (consecutive chars batched)

### Performance Characteristics

**Network Requests:**
- CSS: 1 request per CSS URL (cached in `produceState`)
- Font slices: 1 request per unique font URL (cached globally)
- Example: "Hello 世界" = 1 CSS + 2 font slices

**Memory Usage:**
- FontSliceCache holds ByteArray for each loaded font
- Typical slice: 50-200 KB
- Example session: 10 slices ≈ 1-2 MB

**Rendering Performance:**
- AnnotatedString building: O(n) where n = text length
- Character grouping batches consecutive chars with same font
- Negligible impact vs standard Text

### Known Limitations

1. **Platform Support**: Currently JS/Wasm only (native platforms would need platform-specific HttpClient and Font loading)
2. **Font Formats**: Depends on platform support (WOFF2 on web)
3. **Complex Scripts**: No special handling for ligatures or kerning across font boundaries
4. **Dynamic CSS**: No support for CSS variables or @import
5. **Local Fonts**: `local()` in src is ignored (web-only limitation)

### Future Enhancements

**Potential Features:**
1. Automatic CSS generation with unicode-range from font files
2. Font subsetting tool to subset fonts based on app text
3. Preload optimization by analyzing app text
4. Native platform support (iOS, Android, Desktop)
5. Advanced typography: ligatures, kerning
6. Progressive font loading for large character sets

**API Evolution:**
- SubsetText as primary API (scoped + standalone)
- SubsetTextField for text input
- SubsetButton, SubsetLabel, etc.
- More scoped composables for Material3 text components

### Run Sample App

 - JavaScript: `./gradlew :sample:composeApp:jsBrowserRun`
 - Wasm: `./gradlew :sample:composeApp:wasmJsBrowserRun`

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
