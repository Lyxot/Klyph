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

/**
 * Simple pluggable logger for Klyph.
 *
 * By default, errors are silently ignored. Set [logger] to receive
 * error messages from font loading and CSS parsing operations.
 *
 * Example:
 * ```
 * KlyphLogger.logger = { tag, message, throwable ->
 *     Log.e(tag, message, throwable)  // Android
 * }
 * ```
 */
object KlyphLogger {
    /**
     * Logger callback. Receives a tag, message, and optional throwable.
     * Set to `null` (default) to suppress all log output.
     */
    var logger: ((tag: String, message: String, throwable: Throwable?) -> Unit)? = null

    internal fun error(tag: String, message: String, throwable: Throwable? = null) {
        logger?.invoke(tag, message, throwable)
    }
}
