/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Tappable "Skip intro / recap / outro" pill, ported from mpvRx's player controls. */
package xyz.mpv.rex.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.presentation.components.GroupedListColumn
import androidx.compose.runtime.collectAsState

/**
 * Skip chip shown while a skippable segment is under the playhead. Deliberately independent of
 * [PlayerControls] visibility: a viewer who hid the controls to watch still gets the button.
 */
@Composable
fun BoxScope.SkipSegmentOverlay(viewModel: PlayerViewModel) {
  val segment by viewModel.currentSkippableSegment.collectAsState()
  AnimatedVisibility(
    visible = segment != null,
    enter = fadeIn() + scaleIn(initialScale = 0.92f),
    exit = fadeOut() + scaleOut(targetScale = 0.92f),
    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 132.dp),
  ) {
    val label = segment?.let { stringResource(it.type.labelRes) }.orEmpty()
    FilledTonalButton(
      onClick = viewModel::skipActiveSegment,
      shape = CircleShape,
      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          painter = painterResource(android.R.drawable.ic_media_next),
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
          text = label,
          modifier = Modifier.padding(start = 8.dp),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }
  }
}
