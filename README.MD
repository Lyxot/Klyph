# Klyph

[![CI/CD](https://github.com/Lyxot/Klyph/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/Lyxot/Klyph/actions/workflows/ci-cd.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Intelligent Font Subsetting for Compose Multiplatform**

Klyph is a Kotlin Multiplatform library that brings web-style font subsetting to Compose applications. It dramatically reduces font loading overhead by fetching only the specific character slices needed for your text, making it ideal for applications using large font files like Chinese, Japanese, or Korean typefaces.

**[View Live Demo →](https://Lyxot.github.io/Klyph/)**

## Features

- **Smart Font Subsetting**: Automatically loads only the font slices needed for the characters you're rendering
- **CSS Integration**: Parses standard CSS `@font-face` rules with `unicode-range` descriptors
- **Automatic URL Resolution**: Handles relative URLs in CSS files (e.g., `./font.woff2`, `../fonts/font.woff2`)
- **Global Caching**: Font slices and CSS files are cached across the entire application session with request
  deduplication
- **Lazy Loading**: Font data is loaded on-demand as characters are rendered
- **Compose-First**: Integrates with Compose Multiplatform using familiar scoped APIs
- **Multiplatform Support**: Works on Android, iOS, JVM, JavaScript, and WebAssembly targets

**Android note:** ByteArray-backed fonts use `android.graphics.fonts.Font` on API 29+, and a temp-file fallback on
older Android versions due to platform API limitations.

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
./gradlew :klyph-core:publishToMavenLocal :klyph-css:publishToMavenLocal
```

Then add the dependency to your Compose Multiplatform project:

```kotlin
commonMain.dependencies {
    implementation("xyz.hyli:klyph-core:1.0.0")
    // CSS providers, parsing, and caching
    implementation("xyz.hyli:klyph-css:1.0.0")
}
```

## Quick Start

### Basic Usage with SubsetText

Use `SubsetText` within a `SubsetFontProvider` scope for automatic font subsetting:

```kotlin
import xyz.hyli.klyph.*

@Composable
fun MyApp() {
    SubsetFontProvider(provider = CssUrlFontDescriptorProvider("https://fonts.googleapis.com/css2?family=Noto+Sans+SC")) {
        // SubsetText automatically gets font descriptors from the provider - no need to pass it!
        SubsetText(
            text = "你好世界 Hello World",
            fontSize = 20.sp
        )

        // Multiple SubsetText calls share the same provider
        SubsetText(
            text = "Another text 另一段文字",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
```

**Key Benefits of Scoped API:**

- `SubsetText` is a scoped function within `SubsetFontProvider` - no provider parameter needed!
- Type-safe: SubsetText only works within the provider scope
- Clean API: Similar to how `Row`, `Column` provide scoped modifiers
- Flexible: Provider interface allows CSS URL, CSS content, or custom descriptor sources

`SubsetText` works similarly to Material3's `Text` with a compatible API. It automatically:
- Analyzes each character in your text
- Loads only the necessary font slices
- Applies the correct font to each character using `AnnotatedString`

### Direct Provider Usage (Without Scope)

You can also provide a FontDescriptorProvider directly for one-off usage:

```kotlin
@Composable
fun MyComponent() {
    SubsetText(
       text = "你好世界 Direct provider usage",
       provider = CssUrlFontDescriptorProvider("https://example.com/fonts.css"),
        fontSize = 16.sp
    )
}
```

### Font Weight and Style

`SubsetText` supports standard Text parameters:

```kotlin
@Composable
fun StyledText() {
   SubsetFontProvider(provider = CssUrlFontDescriptorProvider("https://example.com/fonts.css")) {
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

### Klyph's Solution: Provider Interface + Scoped API + Character-Level Font Application

Klyph uses a provider interface pattern combined with scoped API and character-level font application:

**Provider Interface:**

- `FontDescriptorProvider` interface abstracts font descriptor sources
- `CssUrlFontDescriptorProvider` - loads descriptors from CSS URL
- `CssContentFontDescriptorProvider` - parses descriptors from CSS content string
- `StaticFontDescriptorProvider` - provides a static list of pre-constructed descriptors
- Extensible: create custom providers for other descriptor sources

**Scoped API Benefits:**

- `SubsetFontProvider` creates a `SubsetFontScope` with the provider
- `SubsetText` is an extension function on the scope
- Provider is captured once and reused by all SubsetText calls
- Type-safe: SubsetText only available within the provider
- Clean, ergonomic API without parameter repetition

**Character-Level Font Application:**
Klyph's `SubsetText` solves the FontFamily limitation by applying fonts character-by-character:

1. **Provider Loading**: FontDescriptorProvider supplies parsed font descriptors
2. **CSS Parsing**: CSS providers fetch and parse `@font-face` rules with `unicode-range` descriptors
3. **URL Resolution**: Relative URLs in CSS (like `url(./font.woff2)`) are automatically resolved against the CSS file's
   base URL
4. **Character Analysis**: Analyzes each character in your text and groups consecutive characters using the same font
5. **Range Matching**: Each character is matched against unicode-range values to determine which font slice it needs
6. **Lazy Loading**: Only the matched font slices are fetched from the network (or cache)
7. **AnnotatedString Building**: Builds an `AnnotatedString` where each character span gets its own `FontFamily` (
   containing only one font slice)
8. **Rendering**: The standard Text composable renders the AnnotatedString with proper font assignments
9. **Global Caching**: Once a font slice or CSS file is loaded, it's cached globally for the session with request
   deduplication

This approach allows "Hello 你好" to render "Hello" with the Latin font slice and "你好" with the Chinese font slice,
all in the same text element.

### Unicode Range Support

Klyph supports all standard CSS unicode-range formats:
- Single code point: `U+26`
- Range: `U+4E00-9FFF`
- Wildcard: `U+4??` (equivalent to `U+400-4FF`)
- Multiple ranges: `U+0-FF, U+131, U+152-153`

## API Reference

### Core Components

#### `FontDescriptorProvider` (Interface)

```kotlin
interface FontDescriptorProvider {
    suspend fun getDescriptors(): List<FontDescriptor>
}
```

Interface for providing parsed font descriptors from various sources. Implementations include:

**`CssUrlFontDescriptorProvider`** - Loads descriptors from a CSS URL:

```kotlin
val provider = CssUrlFontDescriptorProvider("https://example.com/fonts.css")
```

**`CssContentFontDescriptorProvider`** - Parses descriptors from CSS content string (with caching):

```kotlin
val provider = CssContentFontDescriptorProvider("""
    @font-face {
        font-family: 'MyFont';
        src: url('font.woff2');
        unicode-range: U+0-FF;
    }
""", baseUrl = "https://example.com/")
```

Note: Results are cached using a hash of the content, so repeated calls with the same CSS content won't re-parse.

**`StaticFontDescriptorProvider`** - Provides a static list of pre-constructed descriptors (useful for bundled fonts):

```kotlin
val descriptor = ResourceFontDescriptor(
    resource = Res.font.my_font,
    fontFamily = "MyFont",
    weight = FontWeight.Normal,
    style = FontStyle.Normal,
    unicodeRanges = emptyList()
)
val provider = StaticFontDescriptorProvider(descriptor)
```

#### `SubsetFontProvider` (Scope Provider)

```kotlin
@Composable
fun SubsetFontProvider(
    provider: FontDescriptorProvider,
    fontFamily: FontFamily? = null,
    content: @Composable SubsetFontScope.() -> Unit
)
```

Provides a FontDescriptorProvider scope for font subsetting. Creates a `SubsetFontScope` where the scoped `SubsetText`
function is available.

**Parameters:**

- `provider`: The FontDescriptorProvider that supplies parsed font descriptors
- `fontFamily`: Optional fallback FontFamily for characters not covered by subset fonts
- `content`: Composable content within the SubsetFontScope

**Usage:**
```kotlin
SubsetFontProvider(provider = CssUrlFontDescriptorProvider("https://example.com/fonts.css")) {
    // Within this scope, SubsetText doesn't need provider parameter
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

- Scoped to `SubsetFontProvider` - automatically gets provider from scope
- Compatible with Material3's `Text` API for most common use cases
- Automatically matches characters to font slices via unicode-range
- Builds AnnotatedString with per-character font assignments
- Supports standard Text parameters (color, fontSize, fontWeight, etc.)
- Type-safe: Only available within provider scope

#### `SubsetText` (Standalone - With Provider)

```kotlin
@Composable
fun SubsetText(
    text: String,
    // ... all standard Text parameters ...
    provider: FontDescriptorProvider
): Unit
```

Standalone version that requires explicit FontDescriptorProvider. Use this for one-off cases outside of
`SubsetFontProvider`.

#### `rememberSubsetAnnotatedString` (Low-level API)

**Scoped version (Recommended):**
```kotlin
@Composable
fun SubsetFontScope.rememberSubsetAnnotatedString(
    text: String,
    requestedWeight: FontWeight? = null,
    requestedStyle: FontStyle? = null
): AnnotatedString
```

**Standalone version:**
```kotlin
@Composable
fun rememberSubsetAnnotatedString(
    descriptors: List<FontDescriptor>,
    text: String,
    requestedWeight: FontWeight? = null,
    requestedStyle: FontStyle? = null
): AnnotatedString
```

Low-level composable that provides direct access to the AnnotatedString building process. Use this when you need more
control than `SubsetText` provides. The standalone version accepts a list of parsed font descriptors, allowing you to
use any descriptor source (not just CSS).

**Use Cases:**
- Building custom composables that use subset fonts
- Combining multiple AnnotatedStrings
- Using the result in non-Text contexts
- Applying additional styling to the AnnotatedString

**Example (Scoped):**
```kotlin
@Composable
fun CustomSubsetText() {
   SubsetFontProvider(provider = CssUrlFontDescriptorProvider("https://example.com/fonts.css")) {
        val annotatedString = rememberSubsetAnnotatedString(
            text = "Hello 世界",
            requestedWeight = FontWeight.Normal,
            requestedStyle = FontStyle.Normal
        )

        // Use the AnnotatedString however you like
        Text(
            text = annotatedString,
            fontFamily = myFallbackFont, // Fallback for unmapped characters
            modifier = Modifier.clickable { /* custom behavior */ }
        )
    }
}
```

**Example (Standalone):**
```kotlin
@Composable
fun CustomSubsetText() {
   // Fetch and cache font descriptors from CSS
   val descriptors by produceState(emptyList(), cssUrl) {
      value = CssCache.getOrLoad("https://example.com/fonts.css")
   }

   val annotatedString = rememberSubsetAnnotatedString(
      descriptors = descriptors,
        text = "Hello 世界",
        requestedWeight = FontWeight.Normal,
        requestedStyle = FontStyle.Normal
    )

    Text(
        text = annotatedString,
        fontFamily = myFallbackFont
    )
}
```

### Utility Functions and Caches

#### `getFontCssDescription`

```kotlin
suspend fun getFontCssDescription(url: String): List<FontFace>
```

Fetches and parses CSS to extract `@font-face` rules. Automatically uses `CssCache` (from `klyph-css`) internally for
caching and request deduplication.

#### `FontSliceCache`

Global cache for loaded font slices with request deduplication and monitoring:

```kotlin
object FontSliceCache {
   val descriptors: StateFlow<Map<String, FontDescriptor>>  // All cached font descriptors
   val receivedBytes: StateFlow<Long>                              // Total bytes downloaded
   suspend fun getOrLoad(descriptor: FontDescriptor): FontFamily   // Gets from cache or fetches
   suspend fun preload(descriptors: List<FontDescriptor>)    // Preloads multiple fonts
   suspend fun clear()                                             // Clears all cached fonts (suspend)
   fun clearAsync()                                                // Clears cache asynchronously (fire-and-forget)
}
```

**Request Deduplication**: When multiple concurrent requests are made for the same font URL, only one network request is
performed and all requests share the result.

**Monitoring**: Track which fonts are cached and total bandwidth usage via reactive StateFlows.

**Example:**
If 10 `SubsetText` instances all render "你好" and mount simultaneously, they will all request the same Chinese font
slice. Without deduplication, this would trigger 10 identical network requests. With deduplication, only 1 request is
made and all 10 instances share the result.

**Platform note:** On Android, ByteArray-backed fonts are constructed via `android.graphics.fonts.Font` on API 29+,
with a temp-file fallback on older API levels.

#### `CssCache`

Global cache for parsed CSS files with request deduplication and monitoring:

```kotlin
object CssCache {
   val descriptors: StateFlow<Map<String, List<FontDescriptor>>>  // All cached CSS descriptors
   val receivedBytes: StateFlow<Long>                                    // Total bytes downloaded
   suspend fun getOrLoad(url: String): List<FontDescriptor>        // Gets from cache or fetches+parses
   suspend fun clear()                                                   // Clears all cached CSS (suspend)
   fun clearAsync()                                                      // Clears cache asynchronously (fire-and-forget)
}
```

**Request Deduplication**: When multiple concurrent requests are made for the same CSS URL, only one network request and
parse operation is performed.

**Monitoring**: Track which CSS files are cached and total bandwidth usage via reactive StateFlows.

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
2. **Cache Warming**: Preload font descriptors for common characters:
   ```kotlin
   LaunchedEffect(Unit) {
       val descriptors = getFontCssDescription(cssUrl)
       FontSliceCache.preload(descriptors.filter { /* filter common chars */ })
   }
   ```
3. **Static Text**: For static text that doesn't change, font slices are loaded once and cached
4. **Dynamic Text**: The system efficiently handles dynamic text by only loading new slices as needed
5. **Scoped Provider**: Use `SubsetFontProvider` to wrap multiple `SubsetText` instances that share the same CSS URL for
   better performance
6. **Monitor Usage**: Track bandwidth and cache efficiency using StateFlows:
   ```kotlin
   val fontBytes by FontSliceCache.receivedBytes.collectAsState()
   val cssBytes by CssCache.receivedBytes.collectAsState()
   Text("Downloaded: ${(fontBytes + cssBytes) / 1024} KB")
   ```
7. **Italic Fallback**: When italic fonts aren't available, Klyph automatically falls back to normal style

## Comparison: Traditional vs. Klyph

### Traditional Approach
```kotlin
// Bundles entire font file in the app (can be 10+ MB for CJK fonts)
val fontFamily = FontFamily(Font(Res.font.large_chinese_font))
Text("你好", fontFamily = fontFamily)

// For mixed text, you're limited to one font:
Text("Hello 你好", fontFamily = chineseFont) // "Hello" uses Chinese font (wrong)
Text("Hello 你好", fontFamily = latinFont)   // "你好" renders blank (broken)
```

### Klyph Approach
```kotlin
// Loads only the font slices needed for your text
SubsetFontProvider(cssUrl = "font-subset.css") {
    SubsetText("你好") // No cssUrl parameter - automatically from scope!
}

// Mixed text works - each character gets matched to its font slice:
SubsetFontProvider(cssUrl = "font-subset.css") {
    SubsetText("Hello 你好") // Latin slice for "Hello", Chinese slice for "你好"
    SubsetText("More text 更多文字") // Reuses already-loaded slices
}
```

**Benefits:**

- Dramatically reduced font data transfer (depends on text length)
- Mixed-language text support with character-level font matching
- Character-level font precision
- Clean scoped API - no repetitive cssUrl parameters

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

1) Run `./gradlew :klyph-core:publishToMavenLocal :klyph-css:publishToMavenLocal`
2) Open `~/.m2/repository/xyz/hyli/`

### Publish to MavenCentral

1) Create an account and a namespace on Sonatype:
   https://central.sonatype.org/register/central-portal/#create-an-account
2) Add developer id, name, email and the project url to
   `./klyph-core/build.gradle.kts` and `./klyph-css/build.gradle.kts`
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
5) Run
   `./gradlew :klyph-core:publishAndReleaseToMavenCentral :klyph-css:publishAndReleaseToMavenCentral --no-configuration-cache`

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
