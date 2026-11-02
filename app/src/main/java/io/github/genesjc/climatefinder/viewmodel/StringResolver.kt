package io.github.genesjc.climatefinder.viewmodel

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

