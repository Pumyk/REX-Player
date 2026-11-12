/* SPDX-License-Identifier: AGPL-3.0-or-later
 * AI integration settings, ported from mpvRx. Every control here is wired to the live service:
 * keys are verified against the provider, model lists are fetched and cached per provider, and the
 * feature toggles are the same preferences the rename and subtitle paths read. */
package xyz.mpv.rex.ui.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.koin.compose.koinInject
import xyz.mpv.rex.R
import xyz.mpv.rex.preferences.AiPreferences
import xyz.mpv.rex.preferences.AiProvider
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.presentation.components.GroupPosition
import xyz.mpv.rex.repository.ai.AiModelInfo
import xyz.mpv.rex.repository.ai.AiService
import xyz.mpv.rex.ui.preferences.components.SwitchPreference
import xyz.mpv.rex.ui.utils.LocalBackStack
import xyz.mpv.rex.ui.utils.navigateBack
import androidx.compose.ui.res.stringResource

private fun decodeModels(raw: String): List<AiModelInfo> =
  runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull { index ->
      val item = array.optJSONObject(index) ?: return@mapNotNull null
      val id = item.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
      AiModelInfo(
        id = id,
        displayName = item.optString("displayName").takeIf { it.isNotBlank() } ?: id,
        isFree = item.optBoolean("isFree", false),
      )
    }
  }.getOrDefault(emptyList())

private fun encodeModels(models: List<AiModelInfo>): String {
  val array = JSONArray()
  models.forEach { model ->
    array.put(
      JSONObject()
        .put("id", model.id)
        .put("displayName", model.displayName)
        .put("isFree", model.isFree),
    )
  }
  return array.toString()
}

private fun keyPreference(preferences: AiPreferences, provider: AiProvider) =
  when (provider) {
    AiProvider.OPENCODE -> preferences.openCodeApiKey
    AiProvider.GROQ -> preferences.groqApiKey
    AiProvider.OPENAI -> preferences.openaiApiKey
    AiProvider.ANTHROPIC -> preferences.anthropicApiKey
    AiProvider.OPENROUTER -> preferences.openrouterApiKey
    AiProvider.TOGETHER -> preferences.togetherApiKey
    AiProvider.CUSTOM_OPENAI -> preferences.customOpenAiApiKey
  }

object AiPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val preferences = koinInject<AiPreferences>()
    val aiService = koinInject<AiService>()
    val scope = rememberCoroutineScope()
    val navBarHeight = xyz.mpv.rex.ui.browser.LocalNavigationBarHeight.current

    val enabled by preferences.enabled.collectAsState()
    val provider by preferences.provider.collectAsState()
    val modelPreference = remember(provider) { preferences.selectedModelFor(provider) }
    val modelsPreference = remember(provider) { preferences.availableModelsFor(provider) }
    val selectedModel by modelPreference.collectAsState()
    val modelsRaw by modelsPreference.collectAsState()
    val availableModels = remember(modelsRaw) { decodeModels(modelsRaw) }

    val keyPreference = remember(provider) { keyPreference(preferences, provider) }
    val apiKey by keyPreference.collectAsState()

    val renameEnabled by preferences.renameWithAi.collectAsState()
    val formatEnabled by preferences.subtitleFormatWithAi.collectAsState()
    val customPromptEnabled by preferences.customPromptEnabled.collectAsState()
    val customPrompt by preferences.customPrompt.collectAsState()

    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    var editingKey by remember { mutableStateOf(false) }
    var keyDraft by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var choosingProvider by remember { mutableStateOf(false) }
    var choosingModel by remember { mutableStateOf(false) }
    var editingPrompt by remember { mutableStateOf(false) }
    var promptDraft by remember { mutableStateOf("") }

    fun runTask(successMessage: String, block: suspend () -> Result<String>) {
      if (busy) return
      busy = true
      status = null
      scope.launch {
        val result = runCatching { block() }.getOrElse { Result.failure(it) }
        status = result.getOrElse { "Failed: ${it.message ?: "unknown error"}" }
        busy = false
      }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.pref_ai_title),
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
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = navBarHeight + 16.dp),
      ) {
        item { PreferenceSectionHeader(title = stringResource(R.string.pref_ai_features)) }
        item {
          GroupedPreferenceCard(position = GroupPosition.ONLY) {
            SwitchPreference(
              value = enabled,
              onValueChange = { preferences.enabled.set(it) },
              title = { Text(stringResource(R.string.pref_ai_enable_title)) },
              summary = {
                Text(
                  text = stringResource(
                    if (enabled) R.string.pref_ai_enable_on else R.string.pref_ai_enable_off,
                  ),
                  color = MaterialTheme.colorScheme.outline,
                )
              },
            )
          }
        }

        item { PreferenceSectionHeader(title = stringResource(R.string.pref_ai_provider)) }
        item {
          GroupedPreferenceCard(position = GroupPosition.ONLY) {
            SettingRow(
              title = stringResource(R.string.pref_ai_provider),
              value = provider.displayName,
              onClick = { choosingProvider = true },
            )
          }
        }

        item { PreferenceSectionHeader(title = stringResource(R.string.pref_ai_api_configuration)) }
        item {
          GroupedPreferenceCard(position = GroupPosition.FIRST) {
            SettingRow(
              title = stringResource(R.string.pref_ai_key_for, provider.displayName),
              value = when {
                apiKey.isBlank() -> stringResource(R.string.pref_ai_not_set)
                else -> "••••••••" + apiKey.takeLast(4)
              },
              onClick = {
                keyDraft = apiKey
                keyVisible = false
                editingKey = true
              },
            )
          }
        }
        item {
          GroupedPreferenceCard(position = GroupPosition.MIDDLE) {
            ActionRow(
              title = stringResource(R.string.pref_ai_verify_key),
              icon = { Icon(Icons.Outlined.Key, contentDescription = null) },
              onClick = {
                runTask("") {
                  if (keyPreference.get().isBlank()) {
                    Result.failure(IllegalStateException("Add an API key first"))
                  } else {
                    aiService.verifyKey(provider).map { "Key verified for ${provider.displayName}" }
                  }
                }
              }
            )
          }
        }
        item {
          GroupedPreferenceCard(position = GroupPosition.LAST) {
            ActionRow(
              title = stringResource(R.string.pref_ai_fetch_models),
              icon = {
                Icon(
                  if (busy) Icons.Outlined.Refresh else Icons.Outlined.AutoAwesome,
                  contentDescription = null,
                )
              },
              onClick = {
                runTask("") {
                  aiService.fetchModelsForProvider(provider).map { models ->
                    modelsPreference.set(encodeModels(models))
                    "Fetched ${models.size} models for ${provider.displayName}"
                  }
                }
              },
            )
          }
        }

        item { PreferenceSectionHeader(title = stringResource(R.string.pref_ai_model)) }
        item {
          GroupedPreferenceCard(position = GroupPosition.FIRST) {
            SettingRow(
              title = stringResource(R.string.pref_ai_model),
              value = selectedModel.ifBlank { stringResource(R.string.pref_ai_no_model_selected) },
              onClick = {
                if (availableModels.isEmpty()) {
                  status = "Fetch the model list first"
                } else {
                  choosingModel = true
                }
              },
            )
          }
        }
        item {
          GroupedPreferenceCard(position = GroupPosition.LAST) {
            ActionRow(
              title = stringResource(R.string.pref_ai_verify_model),
              icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
              onClick = {
                runTask("") {
                  if (selectedModel.isBlank()) {
                    Result.failure(IllegalStateException("Pick a model first"))
                  } else {
                    aiService.verifyModel(provider).map { it }
                  }
                }
              },
            )
          }
        }

        if (busy || status != null) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (busy) CircularProgressIndicator(modifier = Modifier.padding(2.dp))
              Text(
                text = if (busy) "Working…" else status.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (status?.startsWith("Failed") == true) {
                  MaterialTheme.colorScheme.error
                } else {
                  MaterialTheme.colorScheme.outline
                },
              )
            }
          }
        }

        item { PreferenceSectionHeader(title = stringResource(R.string.pref_ai_feature_toggles)) }
        item {
          GroupedPreferenceCard(position = GroupPosition.FIRST) {
            SwitchPreference(
              value = renameEnabled,
              onValueChange = { preferences.renameWithAi.set(it) },
              title = { Text(stringResource(R.string.pref_ai_rename_title)) },
              summary = {
                Text(stringResource(R.string.pref_ai_rename_summary), color = MaterialTheme.colorScheme.outline)
              },
            )
          }
        }
        item {
          GroupedPreferenceCard(position = GroupPosition.LAST) {
            SwitchPreference(
              value = formatEnabled,
              onValueChange = { preferences.subtitleFormatWithAi.set(it) },
              title = { Text(stringResource(R.string.pref_ai_search_title)) },
              summary = {
                Text(stringResource(R.string.pref_ai_search_summary), color = MaterialTheme.colorScheme.outline)
              },
            )
          }
        }

        item { PreferenceSectionHeader(title = stringResource(R.string.pref_ai_custom_prompt_section)) }
        item {
          GroupedPreferenceCard(position = GroupPosition.FIRST) {
            SwitchPreference(
              value = customPromptEnabled,
              onValueChange = { preferences.customPromptEnabled.set(it) },
              title = { Text(stringResource(R.string.pref_ai_custom_prompt_title)) },
              summary = {
                Text(
                  text = stringResource(
                    if (customPromptEnabled) R.string.pref_ai_custom_prompt_on else R.string.pref_ai_custom_prompt_off,
                  ),
                  color = MaterialTheme.colorScheme.outline,
                )
              },
            )
          }
        }
        item {
          GroupedPreferenceCard(position = GroupPosition.LAST) {
            SettingRow(
              title = stringResource(R.string.pref_ai_custom_prompt_edit),
              value = if (customPrompt.isBlank()) stringResource(R.string.pref_ai_not_set) else customPrompt.take(60),
              onClick = {
                promptDraft = customPrompt
                editingPrompt = true
              },
            )
          }
        }
      }
    }

    // Tapping the key row only toggled a flag with nothing attached to it, so the field never
    // appeared and the row felt dead.
    if (editingKey) {
      AlertDialog(
        onDismissRequest = { editingKey = false },
        title = { Text(stringResource(R.string.pref_ai_key_dialog_title, provider.displayName)) },
        text = {
          OutlinedTextField(
            value = keyDraft,
            onValueChange = { keyDraft = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.pref_ai_key_hint)) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            visualTransformation =
              if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              TextButton(onClick = { keyVisible = !keyVisible }) {
                Text(
                  stringResource(
                    if (keyVisible) R.string.pref_ai_key_hide else R.string.pref_ai_key_show,
                  ),
                )
              }
            },
          )
        },
        confirmButton = {
          Button(
            onClick = {
              keyPreference.set(keyDraft)
              editingKey = false
              status = null
            },
            shape = MaterialTheme.shapes.extraLarge,
          ) {
            Text(stringResource(R.string.generic_save))
          }
        },
        dismissButton = {
          TextButton(onClick = { editingKey = false }, shape = MaterialTheme.shapes.extraLarge) {
            Text(stringResource(R.string.generic_cancel))
          }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
      )
    }

    if (choosingProvider) {
      ChoiceDialog(
        title = stringResource(R.string.pref_ai_provider),
        options = AiProvider.entries.map { it.displayName },
        selected = provider.displayName,
        onDismiss = { choosingProvider = false },
        onPick = { index ->
          preferences.provider.set(AiProvider.entries[index])
          choosingProvider = false
          status = null
        },
      )
    }

    if (choosingModel) {
      ChoiceDialog(
        title = stringResource(R.string.pref_ai_model),
        options = availableModels.map { it.displayName },
        selected = availableModels.firstOrNull { it.id == selectedModel }?.displayName,
        onDismiss = { choosingModel = false },
        onPick = { index ->
          val model = availableModels.getOrNull(index)
          if (model != null) {
            modelPreference.set(model.id)
            preferences.selectedModel.set(model.id)
          }
          choosingModel = false
        },
      )
    }

    if (editingPrompt) {
      AlertDialog(
        onDismissRequest = { editingPrompt = false },
        title = { Text(stringResource(R.string.pref_ai_custom_prompt_edit)) },
        text = {
          OutlinedTextField(
            value = promptDraft,
            onValueChange = { promptDraft = it },
            label = { Text(stringResource(R.string.pref_ai_custom_prompt_hint)) },
            singleLine = false,
          )
        },
        confirmButton = {
          TextButton(onClick = {
            preferences.customPrompt.set(promptDraft)
            editingPrompt = false
          }) { Text(stringResource(R.string.pref_ai_save)) }
        },
        dismissButton = {
          TextButton(onClick = { editingPrompt = false }) { Text(stringResource(android.R.string.cancel)) }
        },
      )
    }
  }
}

@Composable
private fun SettingRow(
  title: String,
  value: String,
  onClick: () -> Unit,
  trailing: String? = null,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.bodyLarge)
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
      )
    }
    TextButton(onClick = onClick) {
      Text(trailing ?: stringResource(R.string.pref_ai_change))
    }
  }
}

@Composable
private fun ActionRow(
  title: String,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    icon()
    Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    TextButton(onClick = onClick) { Text(stringResource(R.string.pref_ai_run)) }
  }
}

@Composable
private fun ChoiceDialog(
  title: String,
  options: List<String>,
  selected: String?,
  onDismiss: () -> Unit,
  onPick: (Int) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      LazyColumn {
        items(options.size) { index ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = options[index] == selected,
              onClick = { onPick(index) },
            )
            Text(
              text = options[index],
              modifier = Modifier.padding(start = 8.dp),
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
    },
  )
}
