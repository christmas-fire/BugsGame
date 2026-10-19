package com.example.bugsgame.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.bugsgame.R
import com.example.bugsgame.data.RecordDisplayItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordAdapter(context: Context, private val records: List<RecordDisplayItem>) :
    ArrayAdapter<RecordDisplayItem>(context, R.layout.item_record, records) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_record, parent, false)
        val record = records[position]

        val textName = view.findViewById<TextView>(R.id.textRecordRankAndName)
        val textScore = view.findViewById<TextView>(R.id.textRecordScore)
        val textDifficulty = view.findViewById<TextView>(R.id.textRecordDifficulty)
        val textAccuracy = view.findViewById<TextView>(R.id.textRecordAccuracy)
        val textDate = view.findViewById<TextView>(R.id.textRecordDate)

        val zodiacSuffix = if (record.playerZodiac.isNotBlank()) " (${record.playerZodiac})" else ""
        textName.text = "${position + 1}. ${record.playerName}$zodiacSuffix"
        textScore.text = "${record.score} очк."
        textDifficulty.text = "Сложность: ${record.difficulty}"
        textAccuracy.text = String.format(Locale.getDefault(), "Точность: %.1f%%", record.accuracy)

        textDate.text = dateFormat.format(Date(record.finishedAtEpochMillis))

        return view
    }
}
