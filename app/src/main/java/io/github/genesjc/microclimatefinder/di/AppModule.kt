package io.github.genesjc.microclimatefinder.di

import org.koin.dsl.module

/**
 * How the app's shared objects are built, in one place, bottom up: shared plumbing, then repositories (with their
 * data sources inside), then state holders. Every definition is a `single`. Nothing is registered yet; the
 * next steps add the data classes and then the ViewModels' helpers.
 */
val appModule = module {
}
