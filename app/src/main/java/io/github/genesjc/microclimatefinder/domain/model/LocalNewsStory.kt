package io.github.genesjc.microclimatefinder.domain.model

data class LocalNewsStory(
    val title: String,
    val source: String,
    val url: String,
    val published: String,
    val snippet: String = ""
)
