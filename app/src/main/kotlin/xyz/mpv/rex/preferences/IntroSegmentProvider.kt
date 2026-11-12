/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Ported from mpvRx (app.gyrolet.mpvrx.preferences.IntroSegmentProvider). */
package xyz.mpv.rex.preferences

/** Community timestamp databases the player can ask for intro / recap / outro segments. */
enum class IntroSegmentProvider(
  val displayName: String,
  val sourceKey: String,
) {
  INTRO_DB("IntroDB", "introdb"),
  THE_INTRO_DB("TIDB", "theintrodb"),
  ANI_SKIP("AniSkip (Anime)", "aniskip"),
  ANIME_SKIP("Anime Skip", "animeskip"),
  HYBRID("Hybrid (All Providers)", "hybrid"),
  SKIP_DB("SkipDB", "skipdb"),
}
