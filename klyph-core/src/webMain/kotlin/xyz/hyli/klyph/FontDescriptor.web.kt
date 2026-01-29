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
import androidx.compose.ui.text.platform.Font as PlatformFont

internal actual fun createFontFromData(
    data: ByteArray,
    descriptor: FontDescriptor
): Font =
    PlatformFont(
        identity = "${descriptor.fontFamily}-${descriptor.weight.weight}-${descriptor.style}-${descriptor.hashCode()}",
        data = data,
        weight = descriptor.weight,
        style = descriptor.style
    )