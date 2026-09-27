package com.example.bugsgame.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.bugsgame.R
import com.example.bugsgame.model.Bug

class GameView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val bugSize = 100

    private val bugImageNames = listOf("bug", "bug1", "bug2", "bug3")

    // Дефолтная картинка на случай отсутствия какого-то файла
    private val defaultBitmap: Bitmap = run {
        val original = BitmapFactory.decodeResource(resources, R.drawable.bug)
        Bitmap.createScaledBitmap(original, bugSize, bugSize, true)
    }

    // Загружаем и масштабируем все картинки один раз в карту: "имя_файла" -> Bitmap
    private val bugBitmaps: Map<String, Bitmap> = bugImageNames.associateWith { name ->
        val resName = name.removeSuffix(".png")
        val resId = resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId != 0) {
            val original = BitmapFactory.decodeResource(resources, resId)
            Bitmap.createScaledBitmap(original, bugSize, bugSize, true)
        } else {
            defaultBitmap
        }
    }

    var bugs: List<Bug> = emptyList()
    var onBugClick: ((x: Float, y: Float) -> Boolean)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (bug in bugs) {
            // Никакого рандома при отрисовке! Берем строго закрепленную за жуком картинку
            val bitmap = bugBitmaps[bug.imageName.removeSuffix(".png")] ?: defaultBitmap
            canvas.drawBitmap(bitmap, bug.x, bug.y, null)
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
