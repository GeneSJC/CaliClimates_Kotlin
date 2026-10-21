package io.github.genesjc.microclimatefinder.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The one app-wide coroutine scope. State holders that must outlive a screen run their work here, and take it as a
 * default constructor argument so tests can pass their own. A supervisor job keeps one failed task from cancelling
 * the others; heavy work is not done here, repositories switch to their own IO dispatcher.
 */
object AppScope {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
