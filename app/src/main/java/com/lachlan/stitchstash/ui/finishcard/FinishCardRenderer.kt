package com.lachlan.stitchstash.ui.finishcard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class BorderStyle(val key: String, val label: String) {
    FLORAL("floral", "Floral"),
    SCALLOP("scallop", "Scalloped"),
    GRANNY("granny", "Granny squares"),
    SIMPLE("simple", "Simple"),
    ;

    companion object {
        fun from(key: String): BorderStyle =
            values().firstOrNull { it.key == key } ?: FLORAL
    }
}

data class FinishCardSpec(
    val photoPath: String?,
    val patternName: String,
    val colourwayName: String,
    val pieceNumber: Int?,
    val border: BorderStyle,
)

object FinishCardRenderer {

    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    /** Renders the card to a PNG in internal storage. Returns absolute path. */
    suspend fun render(context: Context, spec: FinishCardSpec): String = withContext(Dispatchers.IO) {
        val bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Cream background
        canvas.drawColor(Color.parseColor("#FBF7F2"))

        // Photo area with rounded corners
        val photoTop = 200f
        val photoSide = 80f
        val photoRect = RectF(photoSide, photoTop, WIDTH - photoSide, photoTop + 800f)
        val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5EFE7")
        }
        canvas.drawRoundRect(photoRect, 32f, 32f, photoPaint)

        spec.photoPath?.let { path ->
            val source = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
            if (source != null) {
                val scaled = cropToRect(source, photoRect.width().toInt(), photoRect.height().toInt())
                canvas.save()
                val clipPath = Path().apply { addRoundRect(photoRect, 32f, 32f, Path.Direction.CW) }
                canvas.clipPath(clipPath)
                canvas.drawBitmap(scaled, photoRect.left, photoRect.top, null)
                canvas.restore()
            }
        }

        // Border
        drawBorder(canvas, spec.border)

        // Title text
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A3C32")
            textAlign = Paint.Align.CENTER
            textSize = 64f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText(spec.patternName, WIDTH / 2f, 1100f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7A6F66")
            textAlign = Paint.Align.CENTER
            textSize = 40f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText(spec.colourwayName, WIDTH / 2f, 1170f, subPaint)

        spec.pieceNumber?.let { num ->
            val labelPaint = Paint(subPaint).apply {
                textSize = 36f
                color = Color.parseColor("#9CAE9C")
            }
            canvas.drawText("Piece #$num from my stash", WIDTH / 2f, 1240f, labelPaint)
        }

        // Watermark
        val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B58787")
            textAlign = Paint.Align.CENTER
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        canvas.drawText("Stitch Stash", WIDTH / 2f, 130f, markPaint)

        val dir = File(context.filesDir, "cards").apply { if (!exists()) mkdirs() }
        val file = File(dir, "finish_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        file.absolutePath
    }

    private fun cropToRect(source: Bitmap, w: Int, h: Int): Bitmap {
        val srcRatio = source.width.toFloat() / source.height
        val dstRatio = w.toFloat() / h
        val (cropW, cropH) = if (srcRatio > dstRatio) {
            (source.height * dstRatio).toInt() to source.height
        } else {
            source.width to (source.width / dstRatio).toInt()
        }
        val x = (source.width - cropW) / 2
        val y = (source.height - cropH) / 2
        val cropped = Bitmap.createBitmap(source, x, y, cropW, cropH)
        return Bitmap.createScaledBitmap(cropped, w, h, true)
    }

    private fun drawBorder(canvas: Canvas, style: BorderStyle) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4A5A5")
            this.style = Paint.Style.FILL
        }
        when (style) {
            BorderStyle.FLORAL -> drawFloral(canvas, paint)
            BorderStyle.SCALLOP -> drawScallop(canvas, paint)
            BorderStyle.GRANNY -> drawGranny(canvas, paint)
            BorderStyle.SIMPLE -> drawSimple(canvas, paint)
        }
    }

    private fun drawSimple(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f
        canvas.drawRoundRect(RectF(30f, 30f, WIDTH - 30f, HEIGHT - 30f), 24f, 24f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawScallop(canvas: Canvas, paint: Paint) {
        val r = 36f
        val step = r * 2
        // Top + bottom
        var x = 20f
        while (x + r < WIDTH) {
            canvas.drawCircle(x + r, 20f + r, r, paint)
            canvas.drawCircle(x + r, HEIGHT - 20f - r, r, paint)
            x += step
        }
        // Sides
        var y = 20f + step
        while (y + r < HEIGHT - step) {
            canvas.drawCircle(20f + r, y + r, r, paint)
            canvas.drawCircle(WIDTH - 20f - r, y + r, r, paint)
            y += step
        }
    }

    private fun drawGranny(canvas: Canvas, paint: Paint) {
        val sz = 60f
        val gap = 10f
        val pink = paint.color
        val sage = Color.parseColor("#9CAE9C")
        val gold = Color.parseColor("#E5B873")
        val colors = listOf(pink, sage, gold)
        // Top
        var x = 0f
        var i = 0
        while (x + sz < WIDTH) {
            paint.color = colors[i % colors.size]
            canvas.drawRect(x, 0f, x + sz, sz, paint)
            canvas.drawRect(x, HEIGHT - sz, x + sz, HEIGHT.toFloat(), paint)
            x += sz + gap
            i++
        }
        var y = sz + gap
        i = 1
        while (y + sz < HEIGHT - sz) {
            paint.color = colors[i % colors.size]
            canvas.drawRect(0f, y, sz, y + sz, paint)
            canvas.drawRect(WIDTH - sz, y, WIDTH.toFloat(), y + sz, paint)
            y += sz + gap
            i++
        }
        paint.color = pink
    }

    private fun drawFloral(canvas: Canvas, paint: Paint) {
        val petalR = 18f
        val centerR = 10f
        val spacing = 110f
        val sageColor = Color.parseColor("#9CAE9C")
        val flowerCenters = mutableListOf<Pair<Float, Float>>()
        var x = 60f
        while (x < WIDTH - 40f) {
            flowerCenters += x to 60f
            flowerCenters += x to (HEIGHT - 60f)
            x += spacing
        }
        var y = 170f
        while (y < HEIGHT - 170f) {
            flowerCenters += 60f to y
            flowerCenters += (WIDTH - 60f) to y
            y += spacing
        }
        for ((cx, cy) in flowerCenters) {
            paint.color = Color.parseColor("#D4A5A5")
            for (k in 0..4) {
                val angle = (k * 72.0 - 90.0) * Math.PI / 180.0
                val px = cx + (cos(angle) * petalR * 1.3).toFloat()
                val py = cy + (sin(angle) * petalR * 1.3).toFloat()
                canvas.drawCircle(px, py, petalR, paint)
            }
            paint.color = Color.parseColor("#E5B873")
            canvas.drawCircle(cx, cy, centerR, paint)
            paint.color = sageColor
            canvas.drawCircle(cx - petalR * 1.8f, cy + petalR * 1.8f, 6f, paint)
            canvas.drawCircle(cx + petalR * 1.8f, cy + petalR * 1.8f, 6f, paint)
        }
    }
}
