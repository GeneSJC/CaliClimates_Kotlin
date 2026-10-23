package io.github.genesjc.microclimatefinder

import android.app.Application
import io.github.genesjc.microclimatefinder.ads.adsModule
import io.github.genesjc.microclimatefinder.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** The app. It starts the dependency container once, before any screen or ViewModel exists. */
class CaliClimates : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CaliClimates)
            modules(appModule, adsModule)
        }
    }
}
