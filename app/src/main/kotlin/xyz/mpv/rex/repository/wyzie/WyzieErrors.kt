/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Error taxonomy kept from REX's original Wyzie client so the subtitle sheet can
 * keep telling the user what actually went wrong (missing key, rate limit, dead
 * network) instead of a single generic failure. The search engine underneath is
 * now the one ported from mpvRx; SubtitleSearchService classifies its failures
 * into these kinds.
 */

package xyz.mpv.rex.repository.wyzie

import java.io.IOException

enum class WyzieErrorKind {
    /** No API key configured, and the selected engine needs one. */
    MissingApiKey,

    /** The key was rejected or is not valid for this endpoint. */
    InvalidApiKey,

    /** Rate limited (free tier quota). */
    RateLimited,

    /** Backend returned an unexpected status, an unparseable body, or nothing usable. */
    ServerError,

    /** Could not reach the backend. */
    NetworkError,
}

class WyzieException(
    val kind: WyzieErrorKind,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)
