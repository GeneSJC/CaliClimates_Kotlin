package io.github.genesjc.microclimatefinder.domain

/**
 * Conversions for the units the app shows. Readings arrive metric (NWS); the app leads with US
 * units, since it is about California. One place, so a units toggle can be added later.
 */
object Units {
    fun fahrenheit(celsius: Double): Double = celsius * 9.0 / 5.0 + 32.0
    fun mph(kmh: Double): Double = kmh * 0.621371
    fun inHg(hPa: Double): Double = hPa * 0.0295300
}
