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

class GameView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val dstRect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

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
    var onBugClick: ((x: Float, y: Float) -> Boolean)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (bug in bugs) {
            val bitmap = bugBitmaps[bug.type] ?: fallbackBitmap
            dstRect.set(bug.x, bug.y, bug.x + bug.size, bug.y + bug.size)
            canvas.drawBitmap(bitmap, null, dstRect, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            onBugClick?.invoke(event.x, event.y)
            return true
        }
        return super.onTouchEvent(event)
    }
}
