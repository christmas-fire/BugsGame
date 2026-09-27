package com.example.bugsgame.view

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.bugsgame.R
import com.example.bugsgame.model.Bug

class GameView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val bugBitmap = BitmapFactory.decodeResource(resources, R.drawable.bug)

    var bugs: List<Bug> = emptyList()
    var onBugClick: ((x: Float, y: Float) -> Boolean)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (bug in bugs) {
            canvas.drawBitmap(bugBitmap, bug.x, bug.y, null)
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