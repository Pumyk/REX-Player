/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Ported from mpvRx (https://github.com/Riteshp2001/mpvRx), original path
 * app/src/main/java/app/gyrolet/mpvrx/network/OkHttpCoroutines.kt. Package renamed to xyz.mpv.rex;
 * logic otherwise unchanged unless marked.
 */

package xyz.mpv.rex.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

internal suspend fun Call.awaitResponse(): Response =
  suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(
      object : Callback {
        override fun onFailure(
          call: Call,
          e: IOException,
        ) {
          if (continuation.isActive) continuation.resumeWith(Result.failure(e))
        }

        override fun onResponse(
          call: Call,
          response: Response,
        ) {
          continuation.resume(response) { _, rejectedResponse, _ -> rejectedResponse.close() }
        }
      },
    )
  }