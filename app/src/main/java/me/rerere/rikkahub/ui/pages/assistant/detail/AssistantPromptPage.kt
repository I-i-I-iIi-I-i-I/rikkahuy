package me.rerere.rikkahub.ui.pages.assistant.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.transformers.DefaultPlaceholderProvider
import me.rerere.rikkahub.data.ai.transformers.TemplateTransformer
import me.rerere.rikkahub.data.ai.transformers.TransformerContext
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantAffectScope
import me.rerere.rikkahub.data.model.AssistantRegex
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.model.QuickMessage
import me.rerere.rikkahub.data.model.toMessageNode
import me.rerere.rikkahub.ui.components.message.ChatMessage
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.ui.components.ui.Select
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TextArea
import me.rerere.rikkahub.ui.theme.JetbrainsMono
import me.rerere.rikkahub.utils.UiState
import me.rerere.rikkahub.utils.insertAtCursor
import me.rerere.rikkahub.utils.onError
import me.rerere.rikkahub.utils.onSuccess
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

@Composable
fun AssistantPromptPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(
        parameters = {
            parametersOf(id)
        }
    )
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.assistant_page_tab_prompt))
                },
                navigationIcon = {
                    BackButton()
                }
            )
        }
    ) { innerPadding ->
        AssistantPromptContent(
            modifier = Modifier.padding(innerPadding),
            assistant = assistant,
            settings = settings,
            onUpdate = { vm.update(it) }
        )
    }
}

@Composable
private fun AssistantPromptContent(
    modifier: Modifier = Modifier,
    assistant: Assistant,
    settings: Settings,
    onUpdate: (Assistant) -> Unit
) {
    val context = LocalContext.current
    val templateTransformer = koinInject<TemplateTransformer>()

    // Capture the latest assistant for use in callbacks to prevent stale state issues
    val currentAssistant by rememberUpdatedState(assistant)

    // Hoisted state for lists to prevent data loss during reordering/adding
    val localPresetTexts = remember(assistant.presetMessages) {
        assistant.presetMessages.map { it.toText() }.toMutableStateList()
    }
    val localQuickTitles = remember(assistant.quickMessages) {
        assistant.quickMessages.map { it.title }.toMutableStateList()
    }
    val localQuickContents = remember(assistant.quickMessages) {
        assistant.quickMessages.map { it.content }.toMutableStateList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val systemPromptValue = rememberTextFieldState(
                    initialText = assistant.systemPrompt,
                )
                TextArea(
                    state = systemPromptValue,
                    label = stringResource(R.string.assistant_page_system_prompt),
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.onFocusChanged { focusState ->
                        if (!focusState.isFocused && systemPromptValue.text.toString() != assistant.systemPrompt) {
                            onUpdate(assistant.copy(systemPrompt = systemPromptValue.text.toString()))
                        }
                    }
                )

                Column {
                    Text(
                        text = stringResource(R.string.assistant_page_available_variables),
                        style = MaterialTheme.typography.labelSmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        DefaultPlaceholderProvider.placeholders.forEach { (k, info) ->
                            Tag(
                                onClick = {
                                    systemPromptValue.insertAtCursor("{{$k}}")
                                }
                            ) {
                                info.displayName()
                                Text(": {{$k}}")
                            }
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        ) {
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_message_template))
                },
                content = {
                    var localTemplate by remember(assistant.messageTemplate) {
                        mutableStateOf(assistant.messageTemplate)
                    }
                    OutlinedTextField(
                        value = localTemplate,
                        onValueChange = { localTemplate = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused && localTemplate != assistant.messageTemplate) {
                                    onUpdate(assistant.copy(messageTemplate = localTemplate))
                                }
                            },
                        minLines = 5,
                        maxLines = 15,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 12.sp,
                            fontFamily = JetbrainsMono,
                            lineHeight = 16.sp
                        )
                    )
                },
                description = {
                    Text(stringResource(R.string.assistant_page_message_template_desc))
                    Text(buildAnnotatedString {
                        append(stringResource(R.string.assistant_page_template_variables_label))
                        append(" ")
                        append(stringResource(R.string.assistant_page_template_variable_role))
                        append(": ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("{{ role }}")
                        }
                        append(", ")
                        append(stringResource(R.string.assistant_page_template_variable_message))
                        append(": ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("{{ message }}")
                        }
                        append(", ")
                        append(stringResource(R.string.assistant_page_template_variable_time))
                        append(": ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("{{ time }}")
                        }
                        append(", ")
                        append(stringResource(R.string.assistant_page_template_variable_date))
                        append(": ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("{{ date }}")
                        }
                    })
                }
            )
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.assistant_page_template_preview),
                    style = MaterialTheme.typography.titleSmall
                )
                val rawMessages = listOf(
                    UIMessage.user("你好啊"),
                    UIMessage.assistant("你好，有什么我可以帮你的吗？"),
                )
                val preview by produceState<UiState<List<UIMessage>>>(
                    UiState.Success(rawMessages),
                    assistant
                ) {
                    value = runCatching {
                        UiState.Success(
                            templateTransformer.transform(
                                ctx = TransformerContext(
                                    context = context,
                                    model = Model(modelId = "gpt-4o", displayName = "GPT-4o"),
                                    assistant = assistant,
                                    settings = settings
                                ),
                                messages = rawMessages
                            )
                        )
                    }.getOrElse {
                        UiState.Error(it)
                    }
                }
                preview.onError {
                    Text(
                        text = it.message ?: it.javaClass.name,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                preview.onSuccess {
                    it.fastForEach { message ->
                        ChatMessage(
                            node = message.toMessageNode(),
                            onFork = {},
                            onRegenerate = {},
                            onEdit = {},
                            onShare = {},
                            onDelete = {},
                            onUpdate = {},
                            conversation = Conversation.ofId(Uuid.random())
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        ) {
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_preset_messages))
                },
                description = {
                    Text(stringResource(R.string.assistant_page_preset_messages_desc))
                }
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                assistant.presetMessages.forEachIndexed { index, presetMessage ->
                    key(index) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Select(
                                    options = listOf(MessageRole.USER, MessageRole.ASSISTANT),
                                    selectedOption = presetMessage.role,
                                    onOptionSelected = { role ->
                                        val latest = currentAssistant
                                        if (index < latest.presetMessages.size) {
                                            onUpdate(
                                                latest.copy(
                                                    presetMessages = latest.presetMessages.mapIndexed { i, msg ->
                                                        if (i == index) {
                                                            msg.copy(
                                                                role = role,
                                                                parts = listOf(UIMessagePart.Text(if (i < localPresetTexts.size) localPresetTexts[i] else msg.toText()))
                                                            )
                                                        } else {
                                                            msg.copy(
                                                                parts = listOf(UIMessagePart.Text(if (i < localPresetTexts.size) localPresetTexts[i] else msg.toText()))
                                                            )
                                                        }
                                                    }
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.width(160.dp)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        val latest = currentAssistant
                                        // Sync all before deleting
                                        val updatedMessages = latest.presetMessages.mapIndexed { i, msg ->
                                            msg.copy(parts = listOf(UIMessagePart.Text(if (i < localPresetTexts.size) localPresetTexts[i] else msg.toText())))
                                        }.toMutableList()

                                        if (index < updatedMessages.size) {
                                            updatedMessages.removeAt(index)
                                            onUpdate(latest.copy(presetMessages = updatedMessages))
                                        }
                                    }
                                ) {
                                    Icon(Lucide.X, null)
                                }
                            }

                            val localText = if (index < localPresetTexts.size) localPresetTexts[index] else presetMessage.toText()
                            OutlinedTextField(
                                value = localText,
                                onValueChange = { if (index < localPresetTexts.size) localPresetTexts[index] = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            val latest = currentAssistant
                                            val currentLocalText = if (index < localPresetTexts.size) localPresetTexts[index] else presetMessage.toText()
                                            if (index < latest.presetMessages.size && currentLocalText != latest.presetMessages[index].toText()) {
                                                onUpdate(
                                                    latest.copy(
                                                        presetMessages = latest.presetMessages.mapIndexed { i, msg ->
                                                            if (i == index) {
                                                                msg.copy(parts = listOf(UIMessagePart.Text(currentLocalText)))
                                                            } else {
                                                                msg
                                                            }
                                                        }
                                                    )
                                                )
                                            }
                                        }
                                    },
                                maxLines = 6
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        val latest = currentAssistant
                        val lastRole = latest.presetMessages.lastOrNull()?.role ?: MessageRole.ASSISTANT
                        val nextRole = when (lastRole) {
                            MessageRole.USER -> MessageRole.ASSISTANT
                            MessageRole.ASSISTANT -> MessageRole.USER
                            else -> MessageRole.USER
                        }

                        // Sync all current fields before adding new one
                        val updatedMessages = latest.presetMessages.mapIndexed { i, msg ->
                            msg.copy(parts = listOf(UIMessagePart.Text(if (i < localPresetTexts.size) localPresetTexts[i] else msg.toText())))
                        }.toMutableList()

                        updatedMessages.add(UIMessage(
                            role = nextRole,
                            parts = listOf(UIMessagePart.Text(""))
                        ))

                        onUpdate(latest.copy(presetMessages = updatedMessages))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Lucide.Plus, null)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        ) {
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_quick_messages))
                },
                description = {
                    Text(stringResource(R.string.assistant_page_quick_messages_desc))
                }
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                assistant.quickMessages.forEachIndexed { index, quickMessage ->
                    key(index) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val localTitle = if (index < localQuickTitles.size) localQuickTitles[index] else quickMessage.title
                                OutlinedTextField(
                                    value = localTitle,
                                    onValueChange = { if (index < localQuickTitles.size) localQuickTitles[index] = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .onFocusChanged { focusState ->
                                            if (!focusState.isFocused) {
                                                val latest = currentAssistant
                                                val currentLocalTitle = if (index < localQuickTitles.size) localQuickTitles[index] else quickMessage.title
                                                if (index < latest.quickMessages.size && currentLocalTitle != latest.quickMessages[index].title) {
                                                    onUpdate(
                                                        latest.copy(
                                                            quickMessages = latest.quickMessages.mapIndexed { i, msg ->
                                                                if (i == index) {
                                                                    msg.copy(title = currentLocalTitle)
                                                                } else {
                                                                    msg
                                                                }
                                                            }
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                    label = { Text(stringResource(R.string.assistant_page_quick_message_title)) }
                                )
                                IconButton(
                                    onClick = {
                                        val latest = currentAssistant
                                        // Sync all before deleting
                                        val updatedMessages = latest.quickMessages.mapIndexed { i, msg ->
                                            msg.copy(
                                                title = if (i < localQuickTitles.size) localQuickTitles[i] else msg.title,
                                                content = if (i < localQuickContents.size) localQuickContents[i] else msg.content
                                            )
                                        }.toMutableList()

                                        if (index < updatedMessages.size) {
                                            updatedMessages.removeAt(index)
                                            onUpdate(latest.copy(quickMessages = updatedMessages))
                                        }
                                    }
                                ) {
                                    Icon(Lucide.X, null)
                                }
                            }
                            val localContent = if (index < localQuickContents.size) localQuickContents[index] else quickMessage.content
                            OutlinedTextField(
                                value = localContent,
                                onValueChange = { if (index < localQuickContents.size) localQuickContents[index] = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            val latest = currentAssistant
                                            val currentLocalContent = if (index < localQuickContents.size) localQuickContents[index] else quickMessage.content
                                            if (index < latest.quickMessages.size && currentLocalContent != latest.quickMessages[index].content) {
                                                onUpdate(
                                                    latest.copy(
                                                        quickMessages = latest.quickMessages.mapIndexed { i, msg ->
                                                            if (i == index) {
                                                                msg.copy(content = currentLocalContent)
                                                            } else {
                                                                msg
                                                            }
                                                        }
                                                    )
                                                )
                                            }
                                        }
                                    },
                                maxLines = 6,
                                label = { Text(stringResource(R.string.assistant_page_quick_message_content)) }
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        val latest = currentAssistant
                        // Sync all current fields before adding new one
                        val updatedMessages = latest.quickMessages.mapIndexed { i, msg ->
                            msg.copy(
                                title = if (i < localQuickTitles.size) localQuickTitles[i] else msg.title,
                                content = if (i < localQuickContents.size) localQuickContents[i] else msg.content
                            )
                        }.toMutableList()

                        updatedMessages.add(QuickMessage())
                        onUpdate(latest.copy(quickMessages = updatedMessages))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Lucide.Plus, null)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        ) {
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_regex_title))
                },
                description = {
                    Text(stringResource(R.string.assistant_page_regex_desc))
                }
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                assistant.regexes.forEachIndexed { index, regex ->
                    key(regex.id) {
                        AssistantRegexCard(
                            regex = regex,
                            onUpdate = onUpdate,
                            assistant = assistant,
                            index = index
                        )
                    }
                }
                Button(
                    onClick = {
                        onUpdate(
                            assistant.copy(
                                regexes = assistant.regexes + AssistantRegex(
                                    id = Uuid.random()
                                )
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Lucide.Plus, null)
                }
            }
        }
    }
}

@Composable
private fun AssistantRegexCard(
    regex: AssistantRegex,
    onUpdate: (Assistant) -> Unit,
    assistant: Assistant,
    index: Int
) {
    // Capture the latest assistant for use in callbacks
    val currentAssistant by rememberUpdatedState(assistant)

    // Flag to prevent onFocusChanged from saving during deletion
    var isDeleting by remember { mutableStateOf(false) }

    var expanded by remember {
        mutableStateOf(false)
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = regex.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 200.dp)
                )
                Switch(
                    checked = regex.enabled,
                    onCheckedChange = { enabled ->
                        val latest = currentAssistant
                        if (index < latest.regexes.size) {
                            onUpdate(
                                latest.copy(
                                    regexes = latest.regexes.mapIndexed { i, reg ->
                                        if (i == index) {
                                            reg.copy(enabled = enabled)
                                        } else {
                                            reg
                                        }
                                    }
                                )
                            )
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
                IconButton(
                    onClick = {
                        expanded = !expanded
                    }
                ) {
                    Icon(
                        imageVector = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                        contentDescription = null
                    )
                }
            }

            if (expanded) {

                var localName by remember(regex.name) { mutableStateOf(regex.name) }
                OutlinedTextField(
                    value = localName,
                    onValueChange = { localName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                                if (!focusState.isFocused && !isDeleting && localName != regex.name) {
                                    val latest = currentAssistant
                                    // Only save if the item still exists
                                    if (index < latest.regexes.size && latest.regexes[index].id == regex.id) {
                                    onUpdate(
                                        latest.copy(
                                            regexes = latest.regexes.mapIndexed { i, reg ->
                                                if (i == index) {
                                                    reg.copy(name = localName)
                                                } else {
                                                    reg
                                                }
                                            }
                                        )
                                    )
                                }
                            }
                        },
                    label = { Text(stringResource(R.string.assistant_page_regex_name)) }
                )

                var localFind by remember(regex.findRegex) { mutableStateOf(regex.findRegex) }
                OutlinedTextField(
                    value = localFind,
                    onValueChange = { localFind = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                                if (!focusState.isFocused && !isDeleting && localFind != regex.findRegex) {
                                    val latest = currentAssistant
                                    // Only save if the item still exists
                                    if (index < latest.regexes.size && latest.regexes[index].id == regex.id) {
                                    onUpdate(
                                        latest.copy(
                                            regexes = latest.regexes.mapIndexed { i, reg ->
                                                if (i == index) {
                                                    reg.copy(findRegex = localFind.trim())
                                                } else {
                                                    reg
                                                }
                                            }
                                        )
                                    )
                                }
                            }
                        },
                    label = { Text(stringResource(R.string.assistant_page_regex_find_regex)) },
                    placeholder = { Text("e.g., \\b\\w+@\\w+\\.\\w+\\b") },
                )

                var localReplace by remember(regex.replaceString) { mutableStateOf(regex.replaceString) }
                OutlinedTextField(
                    value = localReplace,
                    onValueChange = { localReplace = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                                if (!focusState.isFocused && !isDeleting && localReplace != regex.replaceString) {
                                    val latest = currentAssistant
                                    // Only save if the item still exists
                                    if (index < latest.regexes.size && latest.regexes[index].id == regex.id) {
                                    onUpdate(
                                        latest.copy(
                                            regexes = latest.regexes.mapIndexed { i, reg ->
                                                if (i == index) {
                                                    reg.copy(replaceString = localReplace)
                                                } else {
                                                    reg
                                                }
                                            }
                                        )
                                    )
                                }
                            }
                        },
                    label = { Text(stringResource(R.string.assistant_page_regex_replace_string)) },
                    placeholder = { Text("e.g., [EMAIL]") }
                )

                Column {
                    Text(
                        text = stringResource(R.string.assistant_page_regex_affecting_scopes),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AssistantAffectScope.entries.forEach { scope ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Checkbox(
                                    checked = scope in regex.affectingScope,
                                    onCheckedChange = { checked ->
                                        val latest = currentAssistant
                                        if (index < latest.regexes.size) {
                                            val newScopes = if (checked) {
                                                regex.affectingScope + scope
                                            } else {
                                                regex.affectingScope - scope
                                            }
                                            onUpdate(
                                                latest.copy(
                                                    regexes = latest.regexes.mapIndexed { i, reg ->
                                                        if (i == index) {
                                                            reg.copy(affectingScope = newScopes)
                                                        } else {
                                                            reg
                                                        }
                                                    }
                                                )
                                            )
                                        }
                                    }
                                )
                                Text(
                                    text = scope.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = regex.visualOnly,
                        onCheckedChange = { visualOnly ->
                            val latest = currentAssistant
                            if (index < latest.regexes.size) {
                                onUpdate(
                                    latest.copy(
                                        regexes = latest.regexes.mapIndexed { i, reg ->
                                            if (i == index) {
                                                reg.copy(visualOnly = visualOnly)
                                            } else {
                                                reg
                                            }
                                        }
                                    )
                                )
                            }
                        }
                    )
                    Text(
                        text = stringResource(R.string.assistant_page_regex_visual_only),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                TextButton(
                    onClick = {
                        isDeleting = true
                        val latest = currentAssistant
                        if (index < latest.regexes.size) {
                            onUpdate(
                                latest.copy(
                                    regexes = latest.regexes.filterIndexed { i, _ ->
                                        i != index
                                    }
                                )
                            )
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Lucide.Trash2, null)
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}
