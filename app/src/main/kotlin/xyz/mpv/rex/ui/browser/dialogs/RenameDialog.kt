package xyz.mpv.rex.ui.browser.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import xyz.mpv.rex.preferences.preference.collectAsState
import androidx.compose.ui.res.stringResource
import xyz.mpv.rex.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RenameDialog(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
  currentName: String,
  @androidx.annotation.StringRes itemTypeRes: Int,
  extension: String? = null,
) {
  if (!isOpen) return

  val baseName = remember(currentName) {
    mutableStateOf(
      TextFieldValue(
        text = currentName,
        selection = TextRange(currentName.length),
      ),
    )
  }
  val isError = remember { mutableStateOf(false) }
  val errorMessage = remember { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }

  // Auto-focus text field
  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  val emptyNameErr = stringResource(R.string.name_cannot_be_empty)
  val invalidCharsErr = stringResource(R.string.name_cannot_contain_invalid_chars)

  fun validateAndConfirm() {
    val text = baseName.value.text
    when {
      text.isBlank() -> {
        isError.value = true
        errorMessage.value = emptyNameErr
      }

      text.contains("/") || text.contains("\\") -> {
        isError.value = true
        errorMessage.value = invalidCharsErr
      }

      else -> {
        onConfirm(text + (extension ?: ""))
        onDismiss()
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.rename_item, stringResource(itemTypeRes)),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        // New name input
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = baseName.value,
            onValueChange = {
              baseName.value = it
              isError.value = false
              errorMessage.value = ""
            },
            modifier =
              Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(R.string.new_name), fontWeight = FontWeight.Medium) },
            singleLine = false,
            maxLines = 5,
            isError = isError.value,
            supportingText =
              if (isError.value) {
                { Text(errorMessage.value) }
              } else {
                null
              },
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
              ),
            keyboardOptions =
              KeyboardOptions(
                imeAction = ImeAction.Done,
              ),
            keyboardActions =
              KeyboardActions(
                onDone = { validateAndConfirm() },
              ),
            shape = MaterialTheme.shapes.extraLarge,
          )

          val aiPreferences = org.koin.compose.koinInject<xyz.mpv.rex.preferences.AiPreferences>()
          val aiService = org.koin.compose.koinInject<xyz.mpv.rex.repository.ai.AiService>()
          val aiOn by aiPreferences.enabled.collectAsState()
          val renameOn by aiPreferences.renameWithAi.collectAsState()
          val scope = rememberCoroutineScope()
          val suggesting = remember { mutableStateOf(false) }
          val aiError = remember { mutableStateOf<String?>(null) }

          if (aiOn && renameOn) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
            ) {
              TextButton(
                enabled = !suggesting.value,
                onClick = {
                  if (suggesting.value) return@TextButton
                  suggesting.value = true
                  aiError.value = null
                  scope.launch {
                    aiService
                      .renameWithAi(currentName, null)
                      .onSuccess { suggestion ->
                        baseName.value = TextFieldValue(suggestion, TextRange(suggestion.length))
                        isError.value = false
                      }
                      .onFailure { aiError.value = it.message ?: "AI rename failed" }
                    suggesting.value = false
                  }
                },
                shape = MaterialTheme.shapes.extraLarge,
              ) {
                Text(
                  text = stringResource(
                    if (suggesting.value) R.string.ai_rename_working else R.string.ai_rename_suggest,
                  ),
                  fontWeight = FontWeight.Medium,
                )
              }
            }
            aiError.value?.let { message ->
              Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { validateAndConfirm() },
        enabled = baseName.value.text.isNotBlank(),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
          ),
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Text(
          text = stringResource(R.string.rename),
          fontWeight = FontWeight.Bold,
        )
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Text(stringResource(R.string.generic_cancel), fontWeight = FontWeight.Medium)
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    shape = MaterialTheme.shapes.extraLarge,
  )
}
