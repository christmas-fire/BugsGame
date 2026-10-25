package com.example.bugsgame.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CbrClient {
    private const val BASE_URL = "https://www.cbr.ru/"

    val api: CbrApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(CbrApiService::class.java)
    }

    suspend fun fetchGoldPrice(): Double = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val today = dateFormat.format(Date())
            val weekAgo = dateFormat.format(Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L))

            val xmlResponse = api.getMetalPrices(weekAgo, today)
            parseGoldPriceFromXml(xmlResponse)
        } catch (e: Exception) {
            e.printStackTrace()
            7500.0
        }
    }

    private fun parseGoldPriceFromXml(xml: String): Double {
        val regex = Regex("""<Record[^>]*Code="1"[^>]*>[\s\S]*?<Buy>([^<]+)</Buy>[\s\S]*?</Record>""")
        val matches = regex.findAll(xml).toList()
        val latestRecord = matches.lastOrNull() ?: return 7500.0

        val priceString = latestRecord.groupValues[1]
            .replace(" ", "")
            .replace(",", ".")
        return priceString.toDoubleOrNull() ?: 7500.0
    }
}