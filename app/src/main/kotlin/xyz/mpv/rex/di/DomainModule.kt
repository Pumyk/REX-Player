package xyz.mpv.rex.di

import xyz.mpv.rex.domain.anime4k.Anime4KManager
import xyz.mpv.rex.domain.hdr.HdrToysManager
import xyz.mpv.rex.preferences.AiPreferences
import xyz.mpv.rex.repository.ai.AiClient
import xyz.mpv.rex.repository.ai.AiService
import xyz.mpv.rex.repository.ai.AnthropicClient
import xyz.mpv.rex.repository.ai.GroqClient
import xyz.mpv.rex.repository.ai.GroqSpeechClient
import xyz.mpv.rex.repository.ai.OpenAiClient
import xyz.mpv.rex.repository.ai.OpenCodeClient
import xyz.mpv.rex.repository.ai.OpenRouterClient
import xyz.mpv.rex.repository.ai.OpenRouterSpeechClient
import xyz.mpv.rex.repository.ai.TogetherClient
import xyz.mpv.rex.repository.IntroDbRepository
import xyz.mpv.rex.repository.subtitle.OnlineSubtitleFileStore
import xyz.mpv.rex.repository.subtitle.OnlineSubtitleOrchestrator
import xyz.mpv.rex.repository.subtitle.SubtitleSearchService
import xyz.mpv.rex.repository.subtitlehub.MpvRxSubtitleHubRepository
import xyz.mpv.rex.repository.wyzie.WyzieSearchRepository
import xyz.mpv.rex.ui.player.managers.PlaybackManager
import xyz.mpv.rex.ui.player.HeadlessPlaybackController
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import java.util.concurrent.TimeUnit

import xyz.mpv.rex.ui.browser.miniplayer.MiniPlayerStateManager

val domainModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    single { Anime4KManager(androidContext()) }
    single { HdrToysManager(androidContext()) }
    single { OnlineSubtitleFileStore(androidContext(), get()) }
    single { WyzieSearchRepository(androidContext(), get(), get(), get(), get()) }
    single { MpvRxSubtitleHubRepository(get(), get(), get(), get()) }
    single {
      OnlineSubtitleOrchestrator(
        wyzieProvider = get<WyzieSearchRepository>(),
        subtitleHubProvider = get<MpvRxSubtitleHubRepository>(),
      )
    }
    single { SubtitleSearchService(get(), get(), get(), get()) }
    single { PlaybackManager(get()) }
    single { MiniPlayerStateManager() }
    single { HeadlessPlaybackController(androidContext()) }
    single { xyz.mpv.rex.jellyfin.api.JellyfinApi(get(), get(), get()) }
    single { xyz.mpv.rex.jellyfin.remote.JellyfinRemoteClient(get(), get(), get(), androidContext()) }
    single { OpenCodeClient(get(), get()) }
    single { GroqClient(get(), get()) }
    single { OpenAiClient(get(), get()) }
    single { AnthropicClient(get(), get()) }
    single { OpenRouterClient(get(), get()) }
    single { TogetherClient(get(), get()) }
    single { GroqSpeechClient(get(), get()) }
    single { OpenRouterSpeechClient(get(), get()) }
    single<AiClient>(named("opencode")) { OpenCodeClient(get(), get()) }
    single<AiClient>(named("groq")) { GroqClient(get(), get()) }
    single<AiClient>(named("openai")) { OpenAiClient(get(), get()) }
    single<AiClient>(named("anthropic")) { AnthropicClient(get(), get()) }
    single<AiClient>(named("openrouter")) { OpenRouterClient(get(), get()) }
    single<AiClient>(named("together")) { TogetherClient(get(), get()) }
    single<AiClient>(named("custom_openai")) {
      val preferences = get<AiPreferences>()
      OpenAiClient(
        get(),
        get(),
        baseUrl = { preferences.customOpenAiBaseUrl.get() },
        providerName = "Custom OpenAI-compatible",
        compatible = true,
      )
    }
    single {
      AiService(
        androidContext(),
        get<AiPreferences>(),
        get<AiClient>(named("opencode")),
        get<AiClient>(named("groq")),
        get<AiClient>(named("openai")),
        get<AiClient>(named("anthropic")),
        get<AiClient>(named("openrouter")),
        get<AiClient>(named("together")),
        get<AiClient>(named("custom_openai")),
        get<Json>(),
      )
    }
    single { IntroDbRepository(get(), get(), androidContext()) }
    single { xyz.mpv.rex.domain.ytdl.YtDlClient(androidContext(), get()) }
}


