/* SPDX-License-Identifier: AGPL-3.0-or-later
 * The wallpaper backdrop mpvRx's liquid glass samples. REX has no wallpaper feature yet, so the
 * local is provided empty and glass falls back to a plain translucent surface. */
package xyz.mpv.rex.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.kyant.backdrop.Backdrop

internal val LocalAppBackgroundBackdrop = staticCompositionLocalOf<Backdrop?> { null }
