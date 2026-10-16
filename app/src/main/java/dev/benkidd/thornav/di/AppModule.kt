package dev.benkidd.thornav.di

import dev.benkidd.thornav.data.UserPreferences
import dev.benkidd.thornav.ui.MainViewModel
import dev.benkidd.thornav.utils.PermissionsHelper
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val appModule = module {
    single { UserPreferences(androidContext()) }
    single { PermissionsHelper(androidContext()) }
    viewModel<MainViewModel>()
}
