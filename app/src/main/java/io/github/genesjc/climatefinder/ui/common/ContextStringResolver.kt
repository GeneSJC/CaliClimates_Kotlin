package io.github.genesjc.climatefinder.ui.common

import android.content.Context

/** The one production [StringResolver]: reads the application's resources, which follow the device language. */
class ContextStringResolver(context: Context) : StringResolver {
    private val resources = context.applicationContext.resources

    override fun invoke(id: Int, vararg args: Any): String = resources.getString(id, *args)

    override fun plural(id: Int, count: Int, vararg args: Any): String = resources.getQuantityString(id, count, *args)
}
