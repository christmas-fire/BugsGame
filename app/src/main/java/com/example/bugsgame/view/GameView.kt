package com.example.bugsgame.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.bugsgame.model.Bug

class GameView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint().apply { color = Color.BLACK }
    var bugs: List<Bug> = emptyList()

    var onBugClick: ((x: Float, y: Float) -> Boolean)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (bug in bugs) {
            canvas.drawRect(bug.x, bug.y, bug.x + bug.size, bug.y + bug.size, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            onBugClick?.invoke(event.x, event.y)
            invalidate()
        }
        return true
    }
}
