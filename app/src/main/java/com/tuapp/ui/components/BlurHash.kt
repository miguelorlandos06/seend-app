package com.tuapp.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlin.math.*

object BlurHash {
    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#\$%*+,-.:;=?@[]^_{|}~"

    fun encode(bitmap: Bitmap, componentsX: Int = 4, componentsY: Int = 4): String {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val factors = Array(componentsX * componentsY) { FloatArray(3) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = pixels[x + y * width]
                val r = Color.red(color) / 255f
                val g = Color.green(color) / 255f
                val b = Color.blue(color) / 255f
                for (j in 0 until componentsY) {
                    for (i in 0 until componentsX) {
                        val basis = (cos(PI * i * x / width) * cos(PI * j * y / height)).toFloat()
                        factors[i + j * componentsX][0] += r * basis
                        factors[i + j * componentsX][1] += g * basis
                        factors[i + j * componentsX][2] += b * basis
                    }
                }
            }
        }

        val maxVal = factors.maxOf { it.maxOf { v -> abs(v) } }
        val quantisedMax = max(0, min(82, floor(maxVal * 166 - 0.5).toInt()))
        val maxValue = (quantisedMax + 1) / 166f
        val dc = factors[0].map { (it / (width * height)).coerceIn(0f, 1f) }
        val sizeFlag = componentsX - 1 + (componentsY - 1) * 9

        val result = StringBuilder()
        result.append(ALPHABET[sizeFlag])
        result.append(ALPHABET[quantisedMax])
        for (c in dc) {
            val v = (c * 255 + 0.5f).toInt().coerceIn(0, 255)
            result.append(ALPHABET[v / 19])
            result.append(ALPHABET[v % 19])
        }
        for (i in 1 until factors.size) {
            val f = factors[i]
            val q = f.map { (it / (width * height * maxValue) * 9 + 9.5f).toInt().coerceIn(0, 18) }
            result.append(ALPHABET[q[0] * 19 * 19 + q[1] * 19 + q[2]])
        }
        return result.toString()
    }

    fun decode(blurHash: String, width: Int = 32, height: Int = 32): Bitmap? {
        if (blurHash.length < 6) return null
        val sizeFlag = ALPHABET.indexOf(blurHash[0])
        val numY = (sizeFlag / 9) + 1
        val numX = (sizeFlag % 9) + 1
        val quantisedMax = ALPHABET.indexOf(blurHash[1])
        val maxValue = (quantisedMax + 1) / 166f

        val colors = Array(numX * numY) { IntArray(3) }
        colors[0] = intArrayOf(
            ALPHABET.indexOf(blurHash[2]) * 19 + ALPHABET.indexOf(blurHash[3]),
            ALPHABET.indexOf(blurHash[4]) * 19 + ALPHABET.indexOf(blurHash[5]),
            ALPHABET.indexOf(blurHash[6]) * 19 + ALPHABET.indexOf(blurHash[7])
        )
        for (i in 1 until colors.size) {
            val idx = 4 + i * 2
            val v = ALPHABET.indexOf(blurHash[idx]) * 19 * 19 + ALPHABET.indexOf(blurHash[idx + 1]) * 19 + ALPHABET.indexOf(blurHash[idx + 2])
            colors[i] = intArrayOf(
                ((v / (19 * 19) - 9) * maxValue * 255).toInt(),
                (((v / 19) % 19 - 9) * maxValue * 255).toInt(),
                ((v % 19 - 9) * maxValue * 255).toInt()
            )
        }
        colors[0] = colors[0].map { it * it }.toIntArray()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0f; var g = 0f; var b = 0f
                for (j in 0 until numY) {
                    for (i in 0 until numX) {
                        val basis = (cos(PI * i * x / width) * cos(PI * j * y / height)).toFloat()
                        r += colors[i + j * numX][0] * basis
                        g += colors[i + j * numX][1] * basis
                        b += colors[i + j * numX][2] * basis
                    }
                }
                pixels[x + y * width] = Color.rgb(r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), b.toInt().coerceIn(0, 255))
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}

@Composable
fun BlurHashImage(blurhash: String, modifier: Modifier = Modifier) {
    val bitmap = remember(blurhash) { BlurHash.decode(blurhash, 64, 64) }
    if (bitmap != null) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}
