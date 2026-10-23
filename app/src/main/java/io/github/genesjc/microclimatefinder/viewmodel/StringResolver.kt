package io.github.genesjc.microclimatefinder.viewmodel

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Turns a string resource into text for the ViewModels, so wording that depends on run-time facts is chosen in one
 * place and the screens just show the result. Tests replace it with a fake that echoes the id and arguments.
 */
interface StringResolver {
    operator fun invoke(@StringRes id: Int, vararg args: Any): String

    fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String
}

/** The one production [StringResolver]: reads the application's resources, which follow the device language. */
class ContextStringResolver(context: Context) : StringResolver {
    private val resources = context.applicationContext.resources

    override fun invoke(id: Int, vararg args: Any): String = resources.getString(id, *args)

    override fun plural(id: Int, count: Int, vararg args: Any): String = resources.getQuantityString(id, count, *args)
}
