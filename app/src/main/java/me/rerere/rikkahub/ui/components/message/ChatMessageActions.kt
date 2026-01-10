package me.rerere.rikkahub.ui.components.message

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.BookOpenText
import com.composables.icons.lucide.CircleStop
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Fingerprint
import com.composables.icons.lucide.GitFork
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Share
import com.composables.icons.lucide.TextSelect
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Volume2
import kotlinx.coroutines.delay
import kotlinx.datetime.toJavaLocalDateTime
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.MessageNode
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalTTSState
import me.rerere.rikkahub.utils.ObfuscationResult
import me.rerere.rikkahub.utils.ObfuscationType
import me.rerere.rikkahub.utils.containsInvisibleChars
import me.rerere.rikkahub.utils.copyMessageToClipboard
import me.rerere.rikkahub.utils.extractQuotedContentAsText
import me.rerere.rikkahub.utils.obfuscate
import me.rerere.rikkahub.utils.toLocalString
import java.util.Locale

@Composable
fun ColumnScope.ChatMessageActionButtons(
    message: UIMessage,
    node: MessageNode,
    onUpdate: (MessageNode) -> Unit,
    onRegenerate: () -> Unit,
    onOpenActionSheet: () -> Unit,
    onTranslate: ((UIMessage, Locale) -> Unit)? = null,
    onClearTranslation: (UIMessage) -> Unit = {},
    // Локальные параметры для обфускации
    onObfuscateAll: (ObfuscationType) -> Unit = {},
    onObfuscateResult: (ObfuscationResult) -> Unit = {},
) {
    val context = LocalContext.current
    var isPendingDelete by remember { mutableStateOf(false) }
    var showTranslateDialog by remember { mutableStateOf(false) }

    // Состояния из локальной версии и сервера
    var showObfuscateDialog by remember { mutableStateOf(false) }
    var showRegenerateConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(isPendingDelete) {
        if (isPendingDelete) {
            delay(3000) // 3 секунды автоотмены
            isPendingDelete = false
        }
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        // Copy Button
        Icon(
            Lucide.Copy, stringResource(R.string.copy), modifier = Modifier
                .clip(CircleShape)
                .clickable { context.copyMessageToClipboard(message) }
                .padding(8.dp)
                .size(16.dp)
        )

        // Regenerate Button (Серверная логика с подтверждением)
        Icon(
            Lucide.RefreshCw, stringResource(R.string.regenerate), modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    if (message.role == MessageRole.USER) {
                        showRegenerateConfirm = true
                    } else {
                        onRegenerate()
                    }
                }
                .padding(8.dp)
                .size(16.dp)
        )

        if (message.role == MessageRole.ASSISTANT) {
            val tts = LocalTTSState.current
            val settings = LocalSettings.current // Используем LocalSettings как на сервере
            val isSpeaking by tts.isSpeaking.collectAsState()
            val isAvailable by tts.isAvailable.collectAsState()

            if (!settings.displaySetting.hideTtsButton) {
                Icon(
                    imageVector = if (isSpeaking) Lucide.CircleStop else Lucide.Volume2,
                    contentDescription = stringResource(R.string.tts),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            enabled = isAvailable,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = {
                                if (!isSpeaking) {
                                    val text = message.toText()
                                    // Серверная логика для чтения цитат
                                    val textToSpeak = if (settings.displaySetting.ttsOnlyReadQuoted) {
                                        text.extractQuotedContentAsText() ?: text
                                    } else {
                                        text
                                    }
                                    tts.speak(textToSpeak)
                                } else {
                                    tts.stop()
                                }
                            }
                        )
                        .padding(8.dp)
                        .size(16.dp),
                    tint = if (isAvailable) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                )
            }

            // Translation button
            if (onTranslate != null) {
                Icon(
                    imageVector = Lucide.Languages,
                    contentDescription = stringResource(R.string.translate),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = {
                                showTranslateDialog = true
                            }
                        )
                        .padding(8.dp)
                        .size(16.dp)
                )
            }
        }

        // Obfuscation button (Локальная фича)
        Icon(
            imageVector = Lucide.EyeOff,
            contentDescription = stringResource(R.string.obfuscate),
            modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = {
                        showObfuscateDialog = true
                    }
                )
                .padding(8.dp)
                .size(16.dp)
        )

        // More Options
        Icon(
            imageVector = Lucide.Ellipsis,
            contentDescription = "More Options",
            modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = {
                        onOpenActionSheet()
                    }
                )
                .padding(8.dp)
                .size(16.dp)
        )

        // Invisible Chars Detector (Локальная фича)
        val hasInvisibleChars = remember(message.parts) {
            message.toText().containsInvisibleChars()
        }
        if (hasInvisibleChars) {
            Icon(
                imageVector = Lucide.Fingerprint,
                contentDescription = null,
                modifier = Modifier
                    .padding(4.dp)
                    .size(10.dp),
                tint = LocalContentColor.current.copy(alpha = 0.5f)
            )
        }

        ChatMessageBranchSelector(
            node = node,
            onUpdate = onUpdate,
        )
    }

    // Translation dialog
    if (showTranslateDialog && onTranslate != null) {
        LanguageSelectionDialog(
            onLanguageSelected = { language ->
                showTranslateDialog = false
                onTranslate(message, language)
            },
            onClearTranslation = {
                showTranslateDialog = false
                onClearTranslation(message)
            },
            onDismissRequest = {
                showTranslateDialog = false
            },
        )
    }

    // Obfuscation dialog (Локальная фича)
    if (showObfuscateDialog) {
        ObfuscationSelectionDialog(
            onOptionSelected = { option, applyToAll ->
                showObfuscateDialog = false
                if (applyToAll) {
                    onObfuscateAll(option)
                } else {
                    onObfuscateResult(node.obfuscate(option))
                }
            },
            onDismissRequest = {
                showObfuscateDialog = false
            },
        )
    }

    // Regenerate confirmation dialog (Серверная фича)
    if (showRegenerateConfirm) {
        AlertDialog(
            onDismissRequest = { showRegenerateConfirm = false },
            title = { Text(stringResource(R.string.regenerate)) },
            text = { Text(stringResource(R.string.regenerate_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRegenerateConfirm = false
                        onRegenerate()
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}


@Composable
fun ObfuscationSelectionDialog(
    onOptionSelected: (ObfuscationType, Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var applyToAll by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.obfuscation_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { applyToAll = !applyToAll }
                    .padding(8.dp)
            ) {
                Checkbox(
                    checked = applyToAll,
                    onCheckedChange = { applyToAll = it }
                )
                Text(
                    text = stringResource(R.string.obfuscation_apply_to_all),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Cyrillic to Latin
            Card(
                onClick = {
                    onOptionSelected(ObfuscationType.CYRILLIC_TO_LATIN, applyToAll)
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Lucide.Languages,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = stringResource(R.string.obfuscation_cyrillic_to_latin),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // Invisible characters
            Card(
                onClick = {
                    onOptionSelected(ObfuscationType.INVISIBLE_CHARS, applyToAll)
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Lucide.EyeOff,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = stringResource(R.string.obfuscation_invisible_chars),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // Homoglyphs
            Card(
                onClick = {
                    onOptionSelected(ObfuscationType.HOMOGLYPHS, applyToAll)
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Lucide.TextSelect,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = stringResource(R.string.obfuscation_homoglyphs),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageActionsSheet(
    message: UIMessage,
    model: Model?,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onFork: () -> Unit,
    onSelectAndCopy: () -> Unit,
    onWebViewPreview: () -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Select and Copy
            Card(
                onClick = {
                    onDismissRequest()
                    onSelectAndCopy()
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Lucide.TextSelect,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = stringResource(R.string.select_and_copy),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // WebView Preview (only show if message has text content)
            val hasTextContent = message.parts.filterIsInstance<UIMessagePart.Text>()
                .any { it.text.isNotBlank() }

            if (hasTextContent) {
                Card(
                    onClick = {
                        onDismissRequest()
                        onWebViewPreview()
                    },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Lucide.BookOpenText,
                            contentDescription = null,
                            modifier = Modifier.padding(4.dp)
                        )
                        Text(
                            text = stringResource(R.string.render_with_webview),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            // Edit
            Card(
                onClick = {
                    onDismissRequest()
                    onEdit()
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Lucide.Pencil,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = stringResource(R.string.edit),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // Share
            Card(
                onClick = {
                    onDismissRequest()
                    onShare()
                },
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Lucide.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = stringResource(R.string.share),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // Create a Fork
            Card(
                onClick = {
                    onDismissRequest()
                    onFork()
                },
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Lucide.GitFork,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = stringResource(R.string.create_fork),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // Delete
            Card(
                onClick = {
                    onDismissRequest()
                    onDelete()
                },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Lucide.Trash2,
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp)
                    )
                    Text(
                        text = stringResource(R.string.delete),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // Message Info
            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                Text(message.createdAt.toJavaLocalDateTime().toLocalString())
                if (model != null) {
                    Text(model.displayName)
                }
            }
        }
    }
}
