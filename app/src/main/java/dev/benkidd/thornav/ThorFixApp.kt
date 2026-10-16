package dev.benkidd.thornav

import android.app.Application
import dev.benkidd.thornav.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ThorFixApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ThorFixApp)
            modules(appModule)
        }
    }
}
