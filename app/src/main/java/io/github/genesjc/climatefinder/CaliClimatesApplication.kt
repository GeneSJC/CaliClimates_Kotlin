package io.github.genesjc.climatefinder

import android.app.Application
import io.github.genesjc.climatefinder.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** The app. It starts the dependency container once, before any screen or ViewModel exists. */
class CaliClimatesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CaliClimatesApplication)
            modules(appModule)
        }
    }
}
