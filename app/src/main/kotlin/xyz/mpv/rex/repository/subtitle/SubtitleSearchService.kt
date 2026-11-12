/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Glue between REX's existing subtitle search UI and the subtitle engine ported
 * from mpvRx (https://github.com/Riteshp2001/mpvRx).
 *
 * REX's screen is unchanged: it still asks for a list of WyzieSubtitle and still
 * downloads by passing one back. What changed is what answers it. Instead of a
 * single Wyzie endpoint that needs an API key, searches now go through
 * OnlineSubtitleOrchestrator, which can hit the keyless subtitle hub sources and
 * merge them with Wyzie when a key is present.
 */

package xyz.mpv.rex.repository.subtitle

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import xyz.mpv.rex.preferences.SubtitlesPreferences
import xyz.mpv.rex.repository.subtitlehub.MpvRxSubtitleHubRepository
import xyz.mpv.rex.repository.wyzie.WyzieSearchRepository
import xyz.mpv.rex.repository.wyzie.WyzieSubtitle
import xyz.mpv.rex.repository.wyzie.WyzieErrorKind
import xyz.mpv.rex.repository.wyzie.WyzieException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class SubtitleSearchService(
  private val orchestrator: OnlineSubtitleOrchestrator,
  private val preferences: SubtitlesPreferences,
  private val wyzieRepository: WyzieSearchRepository,
  private val subtitleHubRepository: MpvRxSubtitleHubRepository,
) {

  /**
   * Results are converted back into [WyzieSubtitle] because that is the model REX's
   * sheet already renders. The untouched [OnlineSubtitle] is kept aside so downloads
   * do not lose provider-specific metadata (SubDL episode ranges, for example).
   */
  private val originals = ConcurrentHashMap<String, OnlineSubtitle>()

  suspend fun search(
    query: String,
    season: Int? = null,
    episode: Int? = null,
    year: String? = null,
  ): Result<List<WyzieSubtitle>> = searchIncremental(query, season, episode, year)

  /**
   * Same search, but [onPartial] fires as each source answers, so the sheet can fill
   * in while slower sites are still being queried. The whole thing is bounded, so the
   * spinner always ends even if every source is unreachable.
   */
  suspend fun searchIncremental(
    query: String,
    season: Int? = null,
    episode: Int? = null,
    year: String? = null,
    onPartial: (List<WyzieSubtitle>) -> Unit = {},
  ): Result<List<WyzieSubtitle>> =
    withContext(Dispatchers.IO) {
      val mode = preferences.onlineSubtitleSearchMode.get()
      val request =
        OnlineSubtitleSearchRequest(
          query = query,
          season = season,
          episode = episode,
          year = year,
        )
      val latest = AtomicReference<List<OnlineSubtitle>>(emptyList())

      val settled =
        withTimeoutOrNull(TOTAL_SEARCH_TIMEOUT_MS) {
          orchestrator.search(
            request = request,
            mode = mode,
            onResults = { partial ->
              latest.set(partial)
              remember(partial)
              if (partial.isNotEmpty()) onPartial(partial.map { it.toWyzieSubtitle() })
            },
          )
        }

      if (settled == null) {
        // Budget spent. Show what arrived rather than spinning forever.
        val partial = latest.get()
        if (partial.isNotEmpty()) {
          Result.success(partial.map { it.toWyzieSubtitle() })
        } else {
          Result.failure(
            WyzieException(
              kind = WyzieErrorKind.NetworkError,
              message = "Subtitle search timed out",
            ),
          )
        }
      } else {
        settled.fold(
          onSuccess = { results ->
            remember(results)
            Result.success(results.map { it.toWyzieSubtitle() })
          },
          onFailure = { error -> Result.failure(classify(error, mode)) },
        )
      }
    }

  private fun remember(results: List<OnlineSubtitle>) {
    results.forEach { originals[it.url] = it }
  }

  suspend fun download(
    subtitle: WyzieSubtitle,
    mediaTitle: String,
  ): Result<Uri> =
    withContext(Dispatchers.IO) {
      val original = originals[subtitle.url]
      if (original == null) {
        // Not from this session's search (or the map was cleared): fall back to the
        // provider recorded on the row.
        return@withContext when (subtitle.provider) {
          SubtitleProvider.WYZIE -> wyzieRepository.download(subtitle, mediaTitle)
          SubtitleProvider.MPVRX_SUBTITLE_HUB ->
            subtitleHubRepository.download(subtitle.toOnlineSubtitle(), mediaTitle)
        }
      }

      when (original.provider) {
        SubtitleProvider.WYZIE -> wyzieRepository.download(subtitle, mediaTitle)
        SubtitleProvider.MPVRX_SUBTITLE_HUB ->
          subtitleHubRepository.download(original, mediaTitle)
      }
    }

  private fun classify(
    error: Throwable,
    mode: OnlineSubtitleSearchMode,
  ): Throwable =
    when {
      error is WyzieException -> error
      mode == OnlineSubtitleSearchMode.WYZIE && preferences.wyzieApiKey.get().isBlank() ->
        WyzieException(
          kind = WyzieErrorKind.MissingApiKey,
          message = "No subtitle API key configured",
          cause = error,
        )
      else ->
        WyzieException(
          kind = if (error is IOException) WyzieErrorKind.NetworkError else WyzieErrorKind.ServerError,
          message = error.message ?: "Subtitle search failed",
          cause = error,
        )
    }

  private fun OnlineSubtitle.toWyzieSubtitle(): WyzieSubtitle =
    WyzieSubtitle(
      id = id,
      url = url,
      format = format,
      encoding = encoding,
      display = displayLanguage.takeIf { it.isNotBlank() },
      language = language ?: displayLanguage,
      media = media,
      isHearingImpaired = isHearingImpaired,
      source = source,
      release = release,
      fileName = fileName,
      downloadCount = downloadCount,
      provider = provider,
    )

  private fun WyzieSubtitle.toOnlineSubtitle(): OnlineSubtitle =
    OnlineSubtitle(
      provider = provider,
      id = id,
      url = url,
      fileName = fileName,
      release = release,
      media = media,
      displayName = displayName,
      displayLanguage = displayLanguage,
      language = language,
      source = source,
      format = format,
      encoding = encoding,
      downloadCount = downloadCount,
      isHearingImpaired = isHearingImpaired,
    )

  private companion object {
    /** Hard ceiling on a search. The sheet stops spinning here no matter what. */
    const val TOTAL_SEARCH_TIMEOUT_MS = 28_000L
  }
}
