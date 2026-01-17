package xyz.hyli.klyph

/**
 * Resolves a potentially relative URL against a base URL.
 *
 * @param baseUrl The base URL (typically the CSS file URL).
 * @param relativeUrl The URL to resolve (may be relative or absolute).
 * @return The resolved absolute URL.
 */
fun resolveUrl(baseUrl: String, relativeUrl: String): String {
    // If the URL is already absolute (has protocol), return as-is
    if (relativeUrl.startsWith("http://") ||
        relativeUrl.startsWith("https://") ||
        relativeUrl.startsWith("//") ||
        relativeUrl.startsWith("data:")) {
        return relativeUrl
    }

    // Parse the base URL
    val baseUrlParts = parseUrl(baseUrl)

    // Handle protocol-relative URLs (//example.com/font.woff2)
    if (relativeUrl.startsWith("//")) {
        return "${baseUrlParts.protocol}:$relativeUrl"
    }

    // Handle absolute paths (/fonts/font.woff2)
    if (relativeUrl.startsWith("/")) {
        return "${baseUrlParts.protocol}://${baseUrlParts.host}$relativeUrl"
    }

    // Handle relative paths (./font.woff2, ../fonts/font.woff2, font.woff2)
    val basePath = baseUrlParts.path.substringBeforeLast('/', "")
    val resolvedPath = resolveRelativePath(basePath, relativeUrl)

    return "${baseUrlParts.protocol}://${baseUrlParts.host}$resolvedPath"
}

/**
 * Parses a URL into its components.
 *
 * @param url The URL to parse.
 * @return A UrlParts object containing the protocol, host, and path.
 */
private fun parseUrl(url: String): UrlParts {
    // Extract protocol
    val protocolEnd = url.indexOf("://")
    if (protocolEnd == -1) {
        // No protocol, treat as relative - shouldn't happen for base URLs
        return UrlParts("https", "", url)
    }

    val protocol = url.substring(0, protocolEnd)
    val afterProtocol = url.substring(protocolEnd + 3)

    // Extract host and path
    val pathStart = afterProtocol.indexOf('/')
    val (host, path) = if (pathStart == -1) {
        // No path, just host
        afterProtocol to ""
    } else {
        afterProtocol.substring(0, pathStart) to afterProtocol.substring(pathStart)
    }

    return UrlParts(protocol, host, path)
}

/**
 * Resolves a relative path against a base path.
 *
 * @param basePath The base path (e.g., "/styles").
 * @param relativePath The relative path (e.g., "./font.woff2" or "../fonts/font.woff2").
 * @return The resolved absolute path.
 */
private fun resolveRelativePath(basePath: String, relativePath: String): String {
    // Split paths into segments
    val baseSegments = basePath.split('/').filter { it.isNotEmpty() }.toMutableList()
    val relativeSegments = relativePath.split('/').filter { it.isNotEmpty() }

    for (segment in relativeSegments) {
        when (segment) {
            "." -> {
                // Current directory, do nothing
            }
            ".." -> {
                // Parent directory, go up one level
                if (baseSegments.isNotEmpty()) {
                    baseSegments.removeAt(baseSegments.lastIndex)
                }
            }
            else -> {
                // Regular segment, add it
                baseSegments.add(segment)
            }
        }
    }

    return "/" + baseSegments.joinToString("/")
}

/**
 * Data class representing the parts of a URL.
 */
private data class UrlParts(
    val protocol: String,
    val host: String,
    val path: String
)
