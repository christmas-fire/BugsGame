package com.example.bugsgame.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.bugsgame.R
import com.example.bugsgame.model.Bug
import com.example.bugsgame.model.BugType
import com.example.bugsgame.model.Bonus

class GameView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val dstRect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val bonusBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD700") }
    private val bonusStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6F00")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val bonusTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 42f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val freezeOverlayPaint = Paint().apply { color = Color.parseColor("#3300E5FF") }
    private val slowOverlayPaint = Paint().apply { color = Color.parseColor("#22FF9800") }

    private val fallbackBitmap: Bitmap by lazy {
        loadBitmapFromDrawable(R.drawable.bug) ?: Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            val c = Canvas(this)
            val p = Paint().apply { color = Color.RED }
            c.drawCircle(50f, 50f, 40f, p)
        }
    }

    private val bugBitmaps = mutableMapOf<BugType, Bitmap>()

    init {
        for (type in BugType.values()) {
            val resId = resources.getIdentifier(type.drawableResName, "drawable", context.packageName)
            val bitmap = if (resId != 0) loadBitmapFromDrawable(resId) else null
            bugBitmaps[type] = bitmap ?: fallbackBitmap
        }
    }

    private fun loadBitmapFromDrawable(resId: Int): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, resId) ?: return null
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return drawable.bitmap
            }
            val w = drawable.intrinsicWidth.coerceAtLeast(100)
            val h = drawable.intrinsicHeight.coerceAtLeast(100)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    var bugs: List<Bug> = emptyList()
    var bonus: Bonus? = null
    var isFrozen: Boolean = false
    var isSlowed: Boolean = false
    var isMultitouchActive: Boolean = false

    var onBugClick: ((x: Float, y: Float) -> Boolean)? = null
    var onMultiTouchClick: ((touches: List<Pair<Float, Float>>) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isFrozen) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), freezeOverlayPaint)
        } else if (isSlowed) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), slowOverlayPaint)
        }

        for (bug in bugs) {
            if (bug.type == BugType.GOLDEN) {
                val bitmap = bugBitmaps[bug.type]
                if (bitmap != null && bitmap != fallbackBitmap) {
                    dstRect.set(bug.x, bug.y, bug.x + bug.size, bug.y + bug.size)
                    canvas.drawBitmap(bitmap, null, dstRect, paint)
                } else {
                    val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD700") }
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#FF8F00")
                        style = Paint.Style.STROKE
                        strokeWidth = 5f
                    }
                    val r = bug.size / 2f
                    canvas.drawCircle(bug.x + r, bug.y + r, r, goldPaint)
                    canvas.drawCircle(bug.x + r, bug.y + r, r, strokePaint)
                    val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#3E2723")
                        textSize = 34f
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    canvas.drawText("Au", bug.x + r, bug.y + r + 12f, textP)
                }
            } else {
                val bitmap = bugBitmaps[bug.type] ?: fallbackBitmap
                dstRect.set(bug.x, bug.y, bug.x + bug.size, bug.y + bug.size)
                canvas.drawBitmap(bitmap, null, dstRect, paint)
            }
        }

        bonus?.let { b ->
            val radius = b.size / 2f
            val cx = b.x + radius
            val cy = b.y + radius
            canvas.drawCircle(cx, cy, radius, bonusBgPaint)
            canvas.drawCircle(cx, cy, radius, bonusStrokePaint)
            canvas.drawText("⭐", cx, cy + 14f, bonusTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (isMultitouchActive) {
                val touches = mutableListOf<Pair<Float, Float>>()
                for (i in 0 until event.pointerCount) {
                    touches.add(Pair(event.getX(i), event.getY(i)))
                }
                onMultiTouchClick?.invoke(touches)
                return true
            } else {
                val pointerIndex = event.actionIndex
                onBugClick?.invoke(event.getX(pointerIndex), event.getY(pointerIndex))
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}