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

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.font.*
import java.io.File
import java.nio.ByteBuffer
import android.graphics.fonts.Font as AndroidPlatformFont
import android.graphics.fonts.FontFamily as AndroidPlatformFontFamily
import android.graphics.fonts.FontStyle as AndroidPlatformFontStyle

actual fun createFontFromData(
    data: ByteArray,
    descriptor: FontDescriptor
): Font =
    AndroidByteBufferFont(
        data = data,
        weight = descriptor.weight,
        style = descriptor.style,
        identity = "${descriptor.fontFamily}-${descriptor.weight.weight}-${descriptor.style}-${descriptor.hashCode()}"
    )

private class AndroidByteBufferFont(
    private val data: ByteArray,
    override val weight: FontWeight,
    override val style: FontStyle,
    private val identity: String,
    variationSettings: FontVariation.Settings = FontVariation.Settings(weight, style),
) : AndroidFont(FontLoadingStrategy.Blocking, ByteBufferTypefaceLoader, variationSettings) {

    private val buffer: ByteBuffer = ByteBuffer.wrap(data).asReadOnlyBuffer()

    @RequiresApi(29)
    fun loadTypefaceFromBuffer(): Typeface {
        buffer.position(0)
        val slant =
            if (style == FontStyle.Italic) {
                AndroidPlatformFontStyle.FONT_SLANT_ITALIC
            } else {
                AndroidPlatformFontStyle.FONT_SLANT_UPRIGHT
            }
        val font = AndroidPlatformFont.Builder(buffer)
            .setWeight(weight.weight)
            .setSlant(slant)
            .build()
        val family = AndroidPlatformFontFamily.Builder(font).build()
        return Typeface.CustomFallbackBuilder(family)
            .setStyle(font.style)
            .build()
    }

    private fun createTempFile(context: Context): File {
        val tempDir = context.cacheDir ?: context.filesDir
        val fileName = "klyph-font-$identity.ttf"
        val tempFile = File(tempDir, fileName)
        if (!tempFile.exists() || tempFile.length() != data.size.toLong()) {
            tempFile.writeBytes(data)
        }
        return tempFile
    }

    @RequiresApi(26)
    fun buildTypefaceFromTempFile(context: Context): Typeface {
        val tempFile = createTempFile(context)
        return Typeface.Builder(tempFile)
            .setWeight(weight.weight)
            .setItalic(style == FontStyle.Italic)
            .build()
    }

    @RequiresApi(4)
    fun createTypefaceFromTempFile(context: Context): Typeface {
        val tempFile = createTempFile(context)
        val base = Typeface.createFromFile(tempFile)
        val wantsBold = weight.weight >= 600
        val wantsItalic = style == FontStyle.Italic
        val styleInt =
            when {
                wantsBold && wantsItalic -> Typeface.BOLD_ITALIC
                wantsBold -> Typeface.BOLD
                wantsItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
        return Typeface.create(base, styleInt)
    }

    override fun toString(): String =
        "Font(byteBuffer, identity=$identity, weight=$weight, style=$style)"
}

private object ByteBufferTypefaceLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
        return if (Build.VERSION.SDK_INT >= 29) {
            (font as AndroidByteBufferFont).loadTypefaceFromBuffer()
        } else if (Build.VERSION.SDK_INT >= 26) {
            (font as AndroidByteBufferFont).buildTypefaceFromTempFile(context)
        } else if (Build.VERSION.SDK_INT >= 4) {
            (font as AndroidByteBufferFont).createTypefaceFromTempFile(context)
        } else {
            throw UnsupportedOperationException("Font loading is not supported on this Android version")
        }
    }

    override suspend fun awaitLoad(
        context: Context,
        font: AndroidFont
    ): Nothing {
        throw UnsupportedOperationException("ByteArray-backed fonts load synchronously.")
    }
}
