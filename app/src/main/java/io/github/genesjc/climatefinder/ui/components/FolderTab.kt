package io.github.genesjc.climatefinder.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * The outline of a folder: a body the full width of [size], and a tab at its top left, [tabWidth] wide and [tabHeight]
 * tall, so the space right of the tab (above the body) is outside the shape. The tab's right side slopes down into the
 * body over [join] with a smooth curve; the corners are rounded by [radius].
 */
fun folderTabPath(size: Size, tabWidth: Float, tabHeight: Float, radius: Float, join: Float): Path {
    val w = size.width
    val h = size.height
    val tabR = radius.coerceAtMost(tabHeight / 2f)
    val tabEnd = tabWidth.coerceAtLeast(tabR * 2f)
    return Path().apply {
        moveTo(0f, tabR)
        quadraticTo(0f, 0f, tabR, 0f)
        lineTo((tabEnd - join).coerceAtLeast(tabR), 0f)
        // The slope from the top of the tab down to the body's top edge
        cubicTo(tabEnd - join * 0.4f, 0f, tabEnd + join * 0.4f, tabHeight, tabEnd + join, tabHeight)
        lineTo(w - radius, tabHeight)
        quadraticTo(w, tabHeight, w, tabHeight + radius)
        lineTo(w, h - radius)
        quadraticTo(w, h, w - radius, h)
        lineTo(radius, h)
        quadraticTo(0f, h, 0f, h - radius)
        close()
    }
}

/**
 * Fills and outlines [folderTabPath] over the whole of the draw area, each fading from its top colour to its bottom colour
 * down the folder: the colours carry the opacity, so a lower alpha at the bottom lets more of what is behind show.
 */
fun DrawScope.drawFolderTab(
    fillTop: Color,
    fillBottom: Color,
    borderTop: Color,
    borderBottom: Color,
    tabWidth: Float,
    tabHeight: Float,
    radius: Float,
    join: Float,
    borderWidth: Float
) {
    val path = folderTabPath(size, tabWidth, tabHeight, radius, join)
    drawPath(path, brush = Brush.verticalGradient(listOf(fillTop, fillBottom), startY = 0f, endY = size.height))
    drawPath(
        path,
        brush = Brush.verticalGradient(listOf(borderTop, borderBottom), startY = 0f, endY = size.height),
        style = Stroke(width = borderWidth)
    )
}
