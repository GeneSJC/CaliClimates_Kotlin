package io.github.genesjc.climatefinder

import android.app.Application
import android.os.StrictMode
import io.github.genesjc.climatefinder.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** The app. It starts the dependency container once, before any screen or ViewModel exists. */
class CaliClimatesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Debug builds only: log (tag StrictMode) any disk or network work done on the main thread, with where it happened,
        // to find the stalls seen on the device (the app stopping for a while, taps answering late)
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        }
        startKoin {
            androidContext(this@CaliClimatesApplication)
            modules(appModule)
        }
    }
}
