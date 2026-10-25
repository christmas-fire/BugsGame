package com.example.bugsgame.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.bugsgame.MainActivity
import com.example.bugsgame.R
import com.example.bugsgame.network.CbrClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class GoldWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_gold_ball)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.textWidgetGoldPrice, pendingIntent)

        CoroutineScope(Dispatchers.IO).launch {
            val price = CbrClient.fetchGoldPrice()
            val formattedPrice = DecimalFormat("#,###").format(price).replace(",", " ") + " ₽"

            withContext(Dispatchers.Main) {
                views.setTextViewText(R.id.textWidgetGoldPrice, formattedPrice)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}