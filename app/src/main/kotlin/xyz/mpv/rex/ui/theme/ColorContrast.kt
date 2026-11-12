/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Ported from mpvRx's AppTheme.kt colour helpers, used by the theme colour-role overrides. */
package xyz.mpv.rex.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private const val MINIMUM_TEXT_CONTRAST = 4.5f

internal fun Color.accessibleContentColor(): Color {
  val blackContrast = contrastRatio(Color.Black)
  val whiteContrast = contrastRatio(Color.White)
  return if (blackContrast >= whiteContrast) Color.Black else Color.White
}

internal fun Color.contrastRatio(background: Color): Float {
  val a = luminance() + 0.05f
  val b = background.luminance() + 0.05f
  return if (a > b) a / b else b / a
}
