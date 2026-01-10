package me.rerere.rikkahub.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import me.rerere.ai.provider.CustomBody
import me.rerere.ai.provider.CustomHeader
import me.rerere.highlight.LocalHighlighter
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.richtext.HighlightCodeVisualTransformation
import me.rerere.rikkahub.ui.theme.JetbrainsMono
import me.rerere.rikkahub.ui.theme.LocalDarkMode

private val jsonLenient = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
}

@Composable
fun CustomHeaders(headers: List<CustomHeader>, onUpdate: (List<CustomHeader>) -> Unit) {
    val currentHeaders by rememberUpdatedState(headers)

    // Local state for all fields - recreated when headers change to sync
    val localNames = remember(headers) {
        headers.map { it.name }.toMutableStateList()
    }
    val localValues = remember(headers) {
        headers.map { it.value }.toMutableStateList()
    }

    val headerNameLabel = stringResource(R.string.assistant_page_header_name)
    val headerValueLabel = stringResource(R.string.assistant_page_header_value)
    val deleteHeaderContentDescription = stringResource(R.string.assistant_page_delete_header)

    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.assistant_page_custom_headers))
        Spacer(Modifier.height(8.dp))

        headers.forEachIndexed { index, header ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = if (index < localNames.size) localNames[index] else header.name,
                            onValueChange = { if (index < localNames.size) localNames[index] = it },
                            label = { Text(headerNameLabel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused) {
                                        val localName = if (index < localNames.size) localNames[index] else header.name
                                        if (localName.trim() != header.name && index < currentHeaders.size) {
                                            val updatedHeaders = currentHeaders.toMutableList()
                                            updatedHeaders[index] = updatedHeaders[index].copy(name = localName.trim())
                                            onUpdate(updatedHeaders)
                                        }
                                    }
                                }
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = if (index < localValues.size) localValues[index] else header.value,
                            onValueChange = { if (index < localValues.size) localValues[index] = it },
                            label = { Text(headerValueLabel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused) {
                                        val localValue = if (index < localValues.size) localValues[index] else header.value
                                        if (localValue.trim() != header.value && index < currentHeaders.size) {
                                            val updatedHeaders = currentHeaders.toMutableList()
                                            updatedHeaders[index] = updatedHeaders[index].copy(value = localValue.trim())
                                            onUpdate(updatedHeaders)
                                        }
                                    }
                                }
                        )
                    }
                    IconButton(onClick = {
                        // Save ALL local changes before deleting
                        val updatedHeaders = currentHeaders.mapIndexed { i, h ->
                            h.copy(
                                name = (if (i < localNames.size) localNames[i] else h.name).trim(),
                                value = (if (i < localValues.size) localValues[i] else h.value).trim()
                            )
                        }.toMutableList()
                        if (index < updatedHeaders.size) {
                            updatedHeaders.removeAt(index)
                            onUpdate(updatedHeaders)
                        }
                    }) {
                        Icon(
                            Lucide.Trash,
                            contentDescription = deleteHeaderContentDescription
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                // Sync ALL local changes before adding new one
                val updatedHeaders = currentHeaders.mapIndexed { i, h ->
                    h.copy(
                        name = (if (i < localNames.size) localNames[i] else h.name).trim(),
                        value = (if (i < localValues.size) localValues[i] else h.value).trim()
                    )
                }.toMutableList()
                updatedHeaders.add(CustomHeader("", ""))
                onUpdate(updatedHeaders)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Lucide.Plus, contentDescription = stringResource(R.string.assistant_page_add_header))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.assistant_page_add_header))
        }
    }
}

@Composable
fun CustomBodies(customBodies: List<CustomBody>, onUpdate: (List<CustomBody>) -> Unit) {
    val context = LocalContext.current
    val currentBodies by rememberUpdatedState(customBodies)

    // Local state for all fields - recreated when customBodies change to sync
    val localKeys = remember(customBodies) {
        customBodies.map { it.key }.toMutableStateList()
    }
    val localValueStrings = remember(customBodies) {
        customBodies.map { jsonLenient.encodeToString(JsonElement.serializer(), it.value) }.toMutableStateList()
    }
    val localValidJsonValues = remember(customBodies) {
        customBodies.map { it.value }.toMutableStateList()
    }
    val localJsonErrors = remember(customBodies) {
        customBodies.map { null as String? }.toMutableStateList()
    }

    val bodyKeyLabel = stringResource(R.string.assistant_page_body_key)
    val bodyValueLabel = stringResource(R.string.assistant_page_body_value)
    val deleteBodyContentDescription = stringResource(R.string.assistant_page_delete_body)

    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.assistant_page_custom_bodies))
        Spacer(Modifier.height(8.dp))

        customBodies.forEachIndexed { index, body ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = if (index < localKeys.size) localKeys[index] else body.key,
                            onValueChange = { if (index < localKeys.size) localKeys[index] = it },
                            label = { Text(bodyKeyLabel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused) {
                                        val localKey = if (index < localKeys.size) localKeys[index] else body.key
                                        if (localKey.trim() != body.key && index < currentBodies.size) {
                                            val updatedBodies = currentBodies.toMutableList()
                                            updatedBodies[index] = updatedBodies[index].copy(key = localKey.trim())
                                            onUpdate(updatedBodies)
                                        }
                                    }
                                }
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = if (index < localValueStrings.size) localValueStrings[index] else "",
                            onValueChange = { newString: String ->
                                if (index < localValueStrings.size) {
                                    localValueStrings[index] = newString
                                    try {
                                        val newJsonValue = jsonLenient.parseToJsonElement(newString)
                                        if (index < localValidJsonValues.size) localValidJsonValues[index] = newJsonValue
                                        if (index < localJsonErrors.size) localJsonErrors[index] = null
                                    } catch (e: Exception) {
                                        if (index < localJsonErrors.size) {
                                            localJsonErrors[index] = context.getString(
                                                R.string.assistant_page_invalid_json,
                                                e.message?.take(100) ?: ""
                                            )
                                        }
                                    }
                                }
                            },
                            label = { Text(bodyValueLabel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused) {
                                        val jsonError = if (index < localJsonErrors.size) localJsonErrors[index] else null
                                        val localValidJson = if (index < localValidJsonValues.size) localValidJsonValues[index] else body.value
                                        if (jsonError == null && localValidJson != body.value && index < currentBodies.size) {
                                            val updatedBodies = currentBodies.toMutableList()
                                            updatedBodies[index] = updatedBodies[index].copy(value = localValidJson)
                                            onUpdate(updatedBodies)
                                        }
                                    }
                                },
                            isError = (if (index < localJsonErrors.size) localJsonErrors[index] else null) != null,
                            supportingText = {
                                val error = if (index < localJsonErrors.size) localJsonErrors[index] else null
                                if (error != null) {
                                    Text(error)
                                }
                            },
                            minLines = 3,
                            maxLines = 5,
                            visualTransformation = HighlightCodeVisualTransformation(
                                language = "json",
                                highlighter = LocalHighlighter.current,
                                darkMode = LocalDarkMode.current
                            ),
                            textStyle = LocalTextStyle.current.merge(fontFamily = JetbrainsMono),
                        )
                    }
                    IconButton(onClick = {
                        // Save ALL local changes before deleting
                        val updatedBodies = currentBodies.mapIndexed { i, b ->
                            val jsonError = if (i < localJsonErrors.size) localJsonErrors[i] else null
                            b.copy(
                                key = (if (i < localKeys.size) localKeys[i] else b.key).trim(),
                                value = if (jsonError == null && i < localValidJsonValues.size) localValidJsonValues[i] else b.value
                            )
                        }.toMutableList()
                        if (index < updatedBodies.size) {
                            updatedBodies.removeAt(index)
                            onUpdate(updatedBodies)
                        }
                    }) {
                        Icon(
                            Lucide.Trash,
                            contentDescription = deleteBodyContentDescription
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                // Sync ALL local changes before adding new one
                val updatedBodies = currentBodies.mapIndexed { i, b ->
                    val jsonError = if (i < localJsonErrors.size) localJsonErrors[i] else null
                    b.copy(
                        key = (if (i < localKeys.size) localKeys[i] else b.key).trim(),
                        value = if (jsonError == null && i < localValidJsonValues.size) localValidJsonValues[i] else b.value
                    )
                }.toMutableList()
                updatedBodies.add(CustomBody("", JsonPrimitive("")))
                onUpdate(updatedBodies)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Lucide.Plus, contentDescription = stringResource(R.string.assistant_page_add_body))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.assistant_page_add_body))
        }
    }
}
