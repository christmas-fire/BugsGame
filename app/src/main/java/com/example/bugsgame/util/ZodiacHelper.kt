package com.example.bugsgame.util

import com.example.bugsgame.R

object ZodiacHelper {
    fun getZodiac(month: Int, day: Int): String {
        return when (month) {
            1 -> if (day < 20) "Козерог" else "Водолей"
            2 -> if (day < 19) "Водолей" else "Рыбы"
            3 -> if (day < 21) "Рыбы" else "Овен"
            4 -> if (day < 20) "Овен" else "Телец"
            5 -> if (day < 21) "Телец" else "Близнецы"
            6 -> if (day < 22) "Близнецы" else "Рак"
            7 -> if (day < 23) "Рак" else "Лев"
            8 -> if (day < 23) "Лев" else "Дева"
            9 -> if (day < 23) "Дева" else "Весы"
            10 -> if (day < 23) "Весы" else "Скорпион"
            11 -> if (day < 22) "Скорпион" else "Стрелец"
            12 -> if (day < 22) "Стрелец" else "Козерог"
            else -> "Неизвестно"
        }
    }

    fun getZodiacImage(month: Int, day: Int): Int {
        return when (getZodiac(month, day)) {
            "Козерог" -> R.drawable.zodiac_1
            "Водолей" -> R.drawable.zodiac_2
            "Рыбы" -> R.drawable.zodiac_3
            "Овен" -> R.drawable.zodiac_4
            "Телец" -> R.drawable.zodiac_5
            "Близнецы" -> R.drawable.zodiac_6
            "Рак" -> R.drawable.zodiac_7
            "Лев" -> R.drawable.zodiac_8
            "Дева" -> R.drawable.zodiac_9
            "Весы" -> R.drawable.zodiac_10
            "Скорпион" -> R.drawable.zodiac_11
            "Стрелец" -> R.drawable.zodiac_12
            else -> R.drawable.zodiac_1
        }
    }
}