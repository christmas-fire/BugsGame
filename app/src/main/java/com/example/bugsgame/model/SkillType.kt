package com.example.bugsgame.model

enum class SkillType(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val price: Int = 550,
    val chargesPerBuy: Int = 3
) {
    FREEZE("freeze", "Заморозка", "Полная остановка всех насекомых на 5 сек", "❄️"),
    BOMB("bomb", "Бомба", "Мгновенное уничтожение всех жуков на экране", "💣"),
    SLOW("slow", "Замедление", "Снижение скорости жуков в 2.5 раза на 7 сек", "⏳"),
    MULTITOUCH("multitouch", "Мультитач-удар", "Ловля несколькими пальцами сразу с ударной волной", "⚡");

    companion object {
        fun fromId(id: String?): SkillType? = values().firstOrNull { it.id == id }
    }
}