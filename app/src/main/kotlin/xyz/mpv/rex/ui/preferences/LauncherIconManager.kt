/* SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Ported from mpvRx's LauncherIconManager (app.gyrolet.mpvrx.ui.preferences).
 * Catalog, splash-theme switching and the enable/disable dance are mpvRx's; alias names and the
 * REX legacy launcher entry are REX's. */
package xyz.mpv.rex.ui.preferences

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import xyz.mpv.rex.R

/** Backdrop of a launcher icon. Each one also needs a color in colors_app_icon.xml. */
enum class IconBackground(val key: String, @StringRes val label: Int) {
  Black("Black", R.string.app_icon_bg_black),
  White("White", R.string.app_icon_bg_white),
  Charcoal("Charcoal", R.string.app_icon_bg_charcoal),
  Indigo("Indigo", R.string.app_icon_bg_indigo),
  Violet("Violet", R.string.app_icon_bg_violet),
  Lagoon("Lagoon", R.string.app_icon_bg_lagoon),
  Azure("Azure", R.string.app_icon_bg_azure),
  Lilac("Lilac", R.string.app_icon_bg_lilac),
  Mint("Mint", R.string.app_icon_bg_mint),
  Ember("Ember", R.string.app_icon_bg_ember),
  Crimson("Crimson", R.string.app_icon_bg_crimson),
  Rose("Rose", R.string.app_icon_bg_rose),
  Peach("Peach", R.string.app_icon_bg_peach),
  Amber("Amber", R.string.app_icon_bg_amber),
  Lemon("Lemon", R.string.app_icon_bg_lemon),
  Lime("Lime", R.string.app_icon_bg_lime),
  Forest("Forest", R.string.app_icon_bg_forest),
  Teal("Teal", R.string.app_icon_bg_teal),
  Sky("Sky", R.string.app_icon_bg_sky),
  Cobalt("Cobalt", R.string.app_icon_bg_cobalt),
  Plum("Plum", R.string.app_icon_bg_plum),
  Cocoa("Cocoa", R.string.app_icon_bg_cocoa),
  Wallpaper("Wallpaper", R.string.app_icon_bg_wallpaper),
}

/** The ribbon drawn over a background, as a 108dp adaptive foreground layer. */
enum class IconMark(val key: String, @StringRes val label: Int) {
  Solid("Solid", R.string.app_icon_logo_solid),
  Gradient("Gradient", R.string.app_icon_logo_gradient),
  White("White", R.string.app_icon_logo_white),
  Black("Black", R.string.app_icon_logo_black),
  Classic("Classic", R.string.app_icon_logo_classic),
}

/**
 * One launcher icon: a background with a mark over it. Only pairs that read well together exist.
 * A new pair needs a row here, the mipmap, a splash style and an activity-alias named [aliasName].
 */
class LauncherIcon(
  val background: IconBackground,
  val mark: IconMark,
  @DrawableRes val preview: Int,
  @StyleRes val splashTheme: Int,
) {
  val aliasName: String get() = "xyz.mpv.rex.Icon${background.key}${mark.key}"

  override fun equals(other: Any?) = other is LauncherIcon &&
    other.background == background && other.mark == mark

  override fun hashCode() = background.hashCode() * 31 + mark.hashCode()

  companion object {
    val entries: List<LauncherIcon> = listOf(
      LauncherIcon(IconBackground.Black, IconMark.Solid, R.mipmap.ic_launcher_black_solid, R.style.Theme_rex_Splash_BlackSolid),
      LauncherIcon(IconBackground.Black, IconMark.Gradient, R.mipmap.ic_launcher_black_gradient, R.style.Theme_rex_Splash_BlackGradient),
      LauncherIcon(IconBackground.Black, IconMark.White, R.mipmap.ic_launcher_black_white, R.style.Theme_rex_Splash_BlackWhite),
      LauncherIcon(IconBackground.White, IconMark.Solid, R.mipmap.ic_launcher_white_solid, R.style.Theme_rex_Splash_WhiteSolid),
      LauncherIcon(IconBackground.White, IconMark.Gradient, R.mipmap.ic_launcher_white_gradient, R.style.Theme_rex_Splash_WhiteGradient),
      LauncherIcon(IconBackground.White, IconMark.Black, R.mipmap.ic_launcher_white_black, R.style.Theme_rex_Splash_WhiteBlack),
      LauncherIcon(IconBackground.Charcoal, IconMark.Solid, R.mipmap.ic_launcher_charcoal_solid, R.style.Theme_rex_Splash_CharcoalSolid),
      LauncherIcon(IconBackground.Charcoal, IconMark.Gradient, R.mipmap.ic_launcher_charcoal_gradient, R.style.Theme_rex_Splash_CharcoalGradient),
      LauncherIcon(IconBackground.Charcoal, IconMark.White, R.mipmap.ic_launcher_charcoal_white, R.style.Theme_rex_Splash_CharcoalWhite),
      LauncherIcon(IconBackground.Indigo, IconMark.Solid, R.mipmap.ic_launcher_indigo_solid, R.style.Theme_rex_Splash_IndigoSolid),
      LauncherIcon(IconBackground.Indigo, IconMark.White, R.mipmap.ic_launcher_indigo_white, R.style.Theme_rex_Splash_IndigoWhite),
      LauncherIcon(IconBackground.Violet, IconMark.Solid, R.mipmap.ic_launcher_violet_solid, R.style.Theme_rex_Splash_VioletSolid),
      LauncherIcon(IconBackground.Violet, IconMark.White, R.mipmap.ic_launcher_violet_white, R.style.Theme_rex_Splash_VioletWhite),
      LauncherIcon(IconBackground.Lagoon, IconMark.White, R.mipmap.ic_launcher_lagoon_white, R.style.Theme_rex_Splash_LagoonWhite),
      LauncherIcon(IconBackground.Lagoon, IconMark.Black, R.mipmap.ic_launcher_lagoon_black, R.style.Theme_rex_Splash_LagoonBlack),
      LauncherIcon(IconBackground.Azure, IconMark.Solid, R.mipmap.ic_launcher_azure_solid, R.style.Theme_rex_Splash_AzureSolid),
      LauncherIcon(IconBackground.Azure, IconMark.Gradient, R.mipmap.ic_launcher_azure_gradient, R.style.Theme_rex_Splash_AzureGradient),
      LauncherIcon(IconBackground.Azure, IconMark.Black, R.mipmap.ic_launcher_azure_black, R.style.Theme_rex_Splash_AzureBlack),
      LauncherIcon(IconBackground.Lilac, IconMark.Solid, R.mipmap.ic_launcher_lilac_solid, R.style.Theme_rex_Splash_LilacSolid),
      LauncherIcon(IconBackground.Lilac, IconMark.Gradient, R.mipmap.ic_launcher_lilac_gradient, R.style.Theme_rex_Splash_LilacGradient),
      LauncherIcon(IconBackground.Lilac, IconMark.Black, R.mipmap.ic_launcher_lilac_black, R.style.Theme_rex_Splash_LilacBlack),
      LauncherIcon(IconBackground.Mint, IconMark.Solid, R.mipmap.ic_launcher_mint_solid, R.style.Theme_rex_Splash_MintSolid),
      LauncherIcon(IconBackground.Mint, IconMark.Gradient, R.mipmap.ic_launcher_mint_gradient, R.style.Theme_rex_Splash_MintGradient),
      LauncherIcon(IconBackground.Mint, IconMark.Black, R.mipmap.ic_launcher_mint_black, R.style.Theme_rex_Splash_MintBlack),
      LauncherIcon(IconBackground.Ember, IconMark.White, R.mipmap.ic_launcher_ember_white, R.style.Theme_rex_Splash_EmberWhite),
      LauncherIcon(IconBackground.Ember, IconMark.Black, R.mipmap.ic_launcher_ember_black, R.style.Theme_rex_Splash_EmberBlack),
      LauncherIcon(IconBackground.Black, IconMark.Classic, R.mipmap.ic_launcher_black_classic, R.style.Theme_rex_Splash_BlackClassic),
      LauncherIcon(IconBackground.White, IconMark.Classic, R.mipmap.ic_launcher_white_classic, R.style.Theme_rex_Splash_WhiteClassic),
      LauncherIcon(IconBackground.Charcoal, IconMark.Classic, R.mipmap.ic_launcher_charcoal_classic, R.style.Theme_rex_Splash_CharcoalClassic),
      LauncherIcon(IconBackground.Indigo, IconMark.Classic, R.mipmap.ic_launcher_indigo_classic, R.style.Theme_rex_Splash_IndigoClassic),
      LauncherIcon(IconBackground.Violet, IconMark.Classic, R.mipmap.ic_launcher_violet_classic, R.style.Theme_rex_Splash_VioletClassic),
      LauncherIcon(IconBackground.Azure, IconMark.Classic, R.mipmap.ic_launcher_azure_classic, R.style.Theme_rex_Splash_AzureClassic),
      LauncherIcon(IconBackground.Lilac, IconMark.Classic, R.mipmap.ic_launcher_lilac_classic, R.style.Theme_rex_Splash_LilacClassic),
      LauncherIcon(IconBackground.Mint, IconMark.Classic, R.mipmap.ic_launcher_mint_classic, R.style.Theme_rex_Splash_MintClassic),
      LauncherIcon(IconBackground.Crimson, IconMark.White, R.mipmap.ic_launcher_crimson_white, R.style.Theme_rex_Splash_CrimsonWhite),
      LauncherIcon(IconBackground.Crimson, IconMark.Black, R.mipmap.ic_launcher_crimson_black, R.style.Theme_rex_Splash_CrimsonBlack),
      LauncherIcon(IconBackground.Rose, IconMark.White, R.mipmap.ic_launcher_rose_white, R.style.Theme_rex_Splash_RoseWhite),
      LauncherIcon(IconBackground.Rose, IconMark.Black, R.mipmap.ic_launcher_rose_black, R.style.Theme_rex_Splash_RoseBlack),
      LauncherIcon(IconBackground.Peach, IconMark.White, R.mipmap.ic_launcher_peach_white, R.style.Theme_rex_Splash_PeachWhite),
      LauncherIcon(IconBackground.Peach, IconMark.Black, R.mipmap.ic_launcher_peach_black, R.style.Theme_rex_Splash_PeachBlack),
      LauncherIcon(IconBackground.Amber, IconMark.White, R.mipmap.ic_launcher_amber_white, R.style.Theme_rex_Splash_AmberWhite),
      LauncherIcon(IconBackground.Amber, IconMark.Black, R.mipmap.ic_launcher_amber_black, R.style.Theme_rex_Splash_AmberBlack),
      LauncherIcon(IconBackground.Lemon, IconMark.White, R.mipmap.ic_launcher_lemon_white, R.style.Theme_rex_Splash_LemonWhite),
      LauncherIcon(IconBackground.Lemon, IconMark.Black, R.mipmap.ic_launcher_lemon_black, R.style.Theme_rex_Splash_LemonBlack),
      LauncherIcon(IconBackground.Lime, IconMark.White, R.mipmap.ic_launcher_lime_white, R.style.Theme_rex_Splash_LimeWhite),
      LauncherIcon(IconBackground.Lime, IconMark.Black, R.mipmap.ic_launcher_lime_black, R.style.Theme_rex_Splash_LimeBlack),
      LauncherIcon(IconBackground.Forest, IconMark.White, R.mipmap.ic_launcher_forest_white, R.style.Theme_rex_Splash_ForestWhite),
      LauncherIcon(IconBackground.Forest, IconMark.Black, R.mipmap.ic_launcher_forest_black, R.style.Theme_rex_Splash_ForestBlack),
      LauncherIcon(IconBackground.Teal, IconMark.White, R.mipmap.ic_launcher_teal_white, R.style.Theme_rex_Splash_TealWhite),
      LauncherIcon(IconBackground.Teal, IconMark.Black, R.mipmap.ic_launcher_teal_black, R.style.Theme_rex_Splash_TealBlack),
      LauncherIcon(IconBackground.Sky, IconMark.White, R.mipmap.ic_launcher_sky_white, R.style.Theme_rex_Splash_SkyWhite),
      LauncherIcon(IconBackground.Sky, IconMark.Black, R.mipmap.ic_launcher_sky_black, R.style.Theme_rex_Splash_SkyBlack),
      LauncherIcon(IconBackground.Cobalt, IconMark.White, R.mipmap.ic_launcher_cobalt_white, R.style.Theme_rex_Splash_CobaltWhite),
      LauncherIcon(IconBackground.Cobalt, IconMark.Black, R.mipmap.ic_launcher_cobalt_black, R.style.Theme_rex_Splash_CobaltBlack),
      LauncherIcon(IconBackground.Plum, IconMark.White, R.mipmap.ic_launcher_plum_white, R.style.Theme_rex_Splash_PlumWhite),
      LauncherIcon(IconBackground.Plum, IconMark.Black, R.mipmap.ic_launcher_plum_black, R.style.Theme_rex_Splash_PlumBlack),
      LauncherIcon(IconBackground.Cocoa, IconMark.White, R.mipmap.ic_launcher_cocoa_white, R.style.Theme_rex_Splash_CocoaWhite),
      LauncherIcon(IconBackground.Cocoa, IconMark.Black, R.mipmap.ic_launcher_cocoa_black, R.style.Theme_rex_Splash_CocoaBlack),
      LauncherIcon(IconBackground.Wallpaper, IconMark.White, R.mipmap.ic_launcher_wallpaper_white, R.style.Theme_rex_Splash_WallpaperWhite),
      LauncherIcon(IconBackground.Wallpaper, IconMark.Black, R.mipmap.ic_launcher_wallpaper_black, R.style.Theme_rex_Splash_WallpaperBlack),
    )

    /** Black with the solid purple ribbon: what the app has always shipped with. */
    val Default: LauncherIcon = of(IconBackground.Black, IconMark.Solid)!!

    fun of(background: IconBackground, mark: IconMark): LauncherIcon? =
      entries.firstOrNull { it.background == background && it.mark == mark }

    /** Marks that read on [background], in catalog order. */
    fun marksFor(background: IconBackground): List<IconMark> =
      entries.filter { it.background == background }.map { it.mark }

    /** The icon for [background] keeping [mark] when it reads there, else the background's own mark. */
    fun resolve(background: IconBackground, mark: IconMark): LauncherIcon =
      of(background, mark) ?: entries.first { it.background == background }
  }
}

/** Backgrounds that offer at least one mark, in catalog order. */
object LauncherIconManager {
  private const val ENABLED = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
  private const val DISABLED = PackageManager.COMPONENT_ENABLED_STATE_DISABLED

  /** REX's original launcher entry, kept so existing installs keep their home-screen shortcut. */
  private const val LEGACY_ALIAS = "app.marlboroadvance.mpvex.MainActivity"

  @Volatile private var cachedCurrent: LauncherIcon? = null
  private var cachedArtwork: Pair<LauncherIcon, Bitmap>? = null

  private fun component(context: Context, name: String) = ComponentName(context.packageName, name)

  private fun stateOf(context: Context, name: String) =
    context.packageManager.getComponentEnabledSetting(component(context, name))

  /** What the package manager says is shown. An explicit pick beats the manifest default. */
  private fun readCurrent(context: Context): LauncherIcon =
    LauncherIcon.entries.firstOrNull { stateOf(context, it.aliasName) == ENABLED } ?: LauncherIcon.Default

  fun current(context: Context): LauncherIcon = cachedCurrent ?: readCurrent(context).also { cachedCurrent = it }

  /** True while the user has never picked an icon, so the original launcher entry still drives. */
  fun usingLegacyEntry(context: Context): Boolean =
    stateOf(context, LEGACY_ALIAS) != DISABLED &&
      LauncherIcon.entries.none { stateOf(context, it.aliasName) == ENABLED }

  /** Android 13+ keeps the splash theme chosen here for the next cold start. */
  private fun applySplashTheme(activity: Activity, icon: LauncherIcon) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    runCatching { activity.splashScreen.setSplashScreenTheme(icon.splashTheme) }
  }

  /** Recents artwork failures must not turn a successful launcher change into an error. */
  @Suppress("DEPRECATION")
  fun refreshTaskIcon(activity: Activity) {
    runCatching { applySplashTheme(activity, current(activity)) }
    runCatching {
      val icon = current(activity)
      val bitmap = cachedArtwork?.takeIf { it.first == icon && icon.background != IconBackground.Wallpaper }?.second
        ?: run {
          val drawable: Drawable = ContextCompat.getDrawable(activity, icon.preview) ?: return@runCatching
          Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(0, 0, it.width, it.height)
            drawable.draw(Canvas(it))
            cachedArtwork = icon to it
          }
        }
      activity.setTaskDescription(ActivityManager.TaskDescription(activity.getString(R.string.app_name), bitmap))
    }
  }

  private fun findActivity(context: Context): Activity? {
    var current: Context = context
    while (current is ContextWrapper) {
      if (current is Activity) return current
      val base = current.baseContext
      if (base === current) break
      current = base
    }
    return current as? Activity
  }

  /** Every alias this manager owns, mapped to the state it should end up in for [icon]. */
  private fun targetStates(context: Context, icon: LauncherIcon, keepLegacy: Boolean): Map<ComponentName, Int> =
    (LauncherIcon.entries.map { it.aliasName } + LEGACY_ALIAS).associate { name ->
      val state = when {
        name == icon.aliasName -> ENABLED
        name == LEGACY_ALIAS && keepLegacy -> ENABLED
        else -> DISABLED
      }
      component(context, name) to state
    }

  private fun applyStates(context: Context, changes: Map<ComponentName, Int>) {
    val pm = context.packageManager
    val flags = PackageManager.DONT_KILL_APP
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.setComponentEnabledSettings(
        changes.map { (name, state) -> PackageManager.ComponentEnabledSetting(name, state, flags) },
      )
    } else {
      // Turn the new one on before the rest go off, so the app never has no launcher entry.
      changes.filter { it.value == ENABLED }.forEach { (name, state) -> pm.setComponentEnabledSetting(name, state, flags) }
      changes.filter { it.value != ENABLED }.forEach { (name, state) -> pm.setComponentEnabledSetting(name, state, flags) }
    }
  }

  /**
   * Switches the launcher icon. Leaving [keepLegacy] on keeps the original launcher entry enabled,
   * which is what a fresh install wants.
   */
  @Synchronized
  @Suppress("DEPRECATION")
  fun select(context: Context, icon: LauncherIcon, keepLegacy: Boolean = false) {
    // Fail before touching anything if this build lacks the alias.
    context.packageManager.getActivityInfo(
      component(context, icon.aliasName),
      PackageManager.MATCH_DISABLED_COMPONENTS,
    )
    val changes = targetStates(context, icon, keepLegacy)
    val previous = changes.keys.associateWith { context.packageManager.getComponentEnabledSetting(it) }
    try {
      applyStates(context, changes)
      check(changes.keys.all { context.packageManager.getComponentEnabledSetting(it) == changes.getValue(it) }) {
        "Launcher alias changes were not applied"
      }
    } catch (failure: Exception) {
      runCatching { applyStates(context, previous) }.exceptionOrNull()?.let(failure::addSuppressed)
      cachedCurrent = null
      throw failure
    }
    cachedCurrent = icon
    findActivity(context)?.let(::refreshTaskIcon)
  }

  /** Repairs a state with none or several aliases enabled. Cheap enough to run on every start. */
  @Synchronized
  fun reconcile(context: Context) {
    val on = LauncherIcon.entries.filter { stateOf(context, it.aliasName) == ENABLED }
    when {
      on.size == 1 -> {
        cachedCurrent = on.first()
        if (stateOf(context, LEGACY_ALIAS) == ENABLED) {
          runCatching {
            context.packageManager.setComponentEnabledSetting(
              component(context, LEGACY_ALIAS),
              DISABLED,
              PackageManager.DONT_KILL_APP,
            )
          }
        }
      }
      on.size > 1 -> runCatching { select(context, on.first(), keepLegacy = false) }
      else -> cachedCurrent = LauncherIcon.Default
    }
  }
}
