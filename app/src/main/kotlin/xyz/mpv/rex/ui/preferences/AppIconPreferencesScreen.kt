/* SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * App icon picker, ported from mpvRx's AppIconPreferencesScreen and rebuilt on REX's preference
 * scaffolding. Tapping a tile switches the launcher alias immediately. */
package xyz.mpv.rex.ui.preferences

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import xyz.mpv.rex.R
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.browser.LocalNavigationBarHeight
import xyz.mpv.rex.ui.utils.LocalBackStack
import xyz.mpv.rex.ui.utils.navigateBack
import kotlinx.serialization.Serializable

@Serializable
object AppIconPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    var selected by remember { mutableStateOf(LauncherIconManager.current(context)) }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.app_icon_screen_title),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            IconButton(onClick = backstack::navigateBack) {
              Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          },
        )
      },
    ) { padding ->
      val navBarHeight = LocalNavigationBarHeight.current
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 84.dp),
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = navBarHeight + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        items(LauncherIcon.entries) { icon ->
          IconTile(
            icon = icon,
            isSelected = icon == selected,
            onClick = {
              try {
                LauncherIconManager.select(context, icon, keepLegacy = false)
                selected = icon
              } catch (error: Exception) {
                Toast.makeText(context, R.string.app_icon_change_failed, Toast.LENGTH_SHORT).show()
              }
            },
          )
        }
      }
    }
  }
}

@Composable
private fun IconTile(icon: LauncherIcon, isSelected: Boolean, onClick: () -> Unit) {
  val context = LocalContext.current
  val bitmap = remember(icon) { loadIconBitmap(context, icon) }
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier = Modifier
        .aspectRatio(1f)
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .then(
          if (isSelected) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(22.dp))
          } else {
            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
          },
        )
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .padding(10.dp)
        .clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
    ) {
      bitmap?.let {
        Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize())
      }
      if (isSelected) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(12.dp),
          )
        }
      }
    }
    Text(
      text = "${stringResource(icon.background.label)} · ${stringResource(icon.mark.label)}",
      style = MaterialTheme.typography.labelSmall,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(top = 6.dp),
    )
  }
}

/** Adaptive-icon XML does not resolve through painterResource on every API level, so rasterise it. */
private fun loadIconBitmap(context: Context, icon: LauncherIcon): ImageBitmap? {
  val drawable = ContextCompat.getDrawable(context, icon.preview) ?: return null
  val size = 160
  val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  drawable.setBounds(0, 0, size, size)
  drawable.draw(canvas)
  return bitmap.asImageBitmap()
}
