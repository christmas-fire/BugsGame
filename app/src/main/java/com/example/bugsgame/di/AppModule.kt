package com.example.bugsgame.di

import com.example.bugsgame.data.AppDatabase
import com.example.bugsgame.data.GameRepository
import com.example.bugsgame.viewmodel.GameViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getInstance(androidContext()) }

    single { GameRepository(get()) }

    viewModel { GameViewModel(repository = get()) }
}
