package et.frectonz.kimem.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import et.frectonz.kimem.R
import et.frectonz.kimem.core.data.NoticeCenter
import et.frectonz.kimem.core.data.RouterRepository
import et.frectonz.kimem.core.data.SettingsRepository
import et.frectonz.kimem.core.model.ActionResult
import et.frectonz.kimem.core.model.Message
import et.frectonz.kimem.core.model.MessageStatus
import et.frectonz.kimem.core.model.MsgSelector
import et.frectonz.kimem.core.model.SmsInbox
import et.frectonz.kimem.core.model.res
import et.frectonz.kimem.core.network.Router
import et.frectonz.kimem.di.container
import et.frectonz.kimem.ui.Load
import et.frectonz.kimem.ui.Crumb
import et.frectonz.kimem.ui.Route
import et.frectonz.kimem.ui.leafCrumbs
import et.frectonz.kimem.ui.menuCrumbs
import et.frectonz.kimem.ui.SmsAction
import et.frectonz.kimem.ui.components.ButtonRow
import et.frectonz.kimem.ui.components.ErrorBox
import et.frectonz.kimem.ui.components.KeyValueTable
import et.frectonz.kimem.ui.components.LoadingBox
import et.frectonz.kimem.ui.components.MonoBox
import et.frectonz.kimem.ui.components.MonoButton
import et.frectonz.kimem.ui.components.MonoDialog
import et.frectonz.kimem.ui.components.MonoDivider
import et.frectonz.kimem.ui.components.MonoIconButton
import et.frectonz.kimem.ui.components.MonoListItem
import et.frectonz.kimem.ui.components.MonoTag
import et.frectonz.kimem.ui.components.MonoTextField
import et.frectonz.kimem.ui.components.MonoTopBar
import et.frectonz.kimem.ui.components.ink
import et.frectonz.kimem.ui.components.paper
import et.frectonz.kimem.ui.load
import et.frectonz.kimem.ui.string
import et.frectonz.kimem.ui.uiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private suspend fun RouterRepository.runAction(
    notices: NoticeCenter,
    emptyNotice: Int,
    block: suspend Router.() -> ActionResult?,
): Boolean = try {
    val result = session(block)
    if (result == null) {
        notices.post(res(emptyNotice))
        false
    } else {
        notices.post(result.notice())
        notifyInboxChanged()
        true
    }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    notices.post(e.uiText())
    false
}

class SmsInboxViewModel(private val repository: RouterRepository) : ViewModel() {
    private val state = MutableStateFlow<Load<SmsInbox>>(Load.Loading)
    val uiState: StateFlow<Load<SmsInbox>> = state.asStateFlow()

    init {
        load()
        viewModelScope.launch { repository.inboxChanges.collect { load() } }
    }

    fun load() {
        viewModelScope.launch {
            state.value = Load.Loading
            state.value = load { repository.session { smsInbox() } }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SmsInboxViewModel(container.routerRepository) }
        }
    }
}

@Composable
fun SmsInboxScreen(
    onNavigate: (Route) -> Unit,
    onCrumb: (Route) -> Unit,
    onBack: () -> Unit,
    vm: SmsInboxViewModel = viewModel(factory = SmsInboxViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = paper(),
        topBar = {
            MonoTopBar(leafCrumbs(listOf("get", "sms", "list")), onCrumb = onCrumb, onBack = onBack) {
                MonoIconButton(Icons.Filled.Refresh, stringResource(R.string.refresh), onClick = vm::load, enabled = state !is Load.Loading)
            }
        },
    ) { inner ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
        ) {
            when (val current = state) {
                Load.Loading -> item { LoadingBox(stringResource(R.string.fetching, stringResource(R.string.inbox))) }
                is Load.Error -> item { ErrorBox(current.message.string(), onRetry = vm::load) }
                is Load.Ready -> {
                    val inbox = current.value
                    if (inbox.messages.isEmpty()) {
                        item {
                            MonoBox(Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.inbox_empty), style = MaterialTheme.typography.bodyMedium, color = ink())
                            }
                        }
                    } else {
                        item {
                            val ink = ink()
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, ink)
                            ) {
                                inbox.messages.asReversed().forEachIndexed { index, message ->
                                    if (index > 0) MonoDivider()
                                    MessageRow(message) { onNavigate(Route.SmsMessage(message.id)) }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MessageRow(message: Message, onClick: () -> Unit) {
    val preview = message.content.trim().replace('\n', ' ')
    MonoListItem(
        title = message.number,
        subtitle = "${message.date.display()}\n${preview.ifEmpty { stringResource(R.string.dash) }}",
        onClick = onClick,
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                MonoTag(message.tag.label.string(), inverted = message.tag is MessageStatus.Unread)
                Spacer(Modifier.height(4.dp))
                Text("#${message.id}", style = MaterialTheme.typography.labelSmall, color = ink())
            }
        },
    )
}

data class SmsMessageUiState(
    val message: Load<Message> = Load.Loading,
    val busy: Boolean = false,
    val deleted: Boolean = false,
)

class SmsMessageViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: RouterRepository,
    private val notices: NoticeCenter,
) : ViewModel() {
    val id: Int = savedStateHandle.toRoute<Route.SmsMessage>().id

    private val state = MutableStateFlow(SmsMessageUiState())
    val uiState: StateFlow<SmsMessageUiState> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            state.update { it.copy(message = Load.Loading) }
            val loaded = load { repository.session { sms(id) } }
            state.update { it.copy(message = loaded) }
        }
    }

    fun markRead() = act(afterwards = ::load) { markSms(listOf(id)) }

    fun delete() = act(afterwards = { state.update { it.copy(deleted = true) } }) { deleteSms(listOf(id)) }

    private fun act(afterwards: () -> Unit, block: suspend Router.() -> ActionResult) {
        if (state.value.busy) return
        state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                if (repository.runAction(notices, R.string.no_entries, block)) afterwards()
            } finally {
                state.update { it.copy(busy = false) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SmsMessageViewModel(createSavedStateHandle(), container.routerRepository, container.notices) }
        }
    }
}

@Composable
fun SmsMessageScreen(
    onNavigate: (Route) -> Unit,
    onCrumb: (Route) -> Unit,
    onBack: () -> Unit,
    vm: SmsMessageViewModel = viewModel(factory = SmsMessageViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    Scaffold(
        containerColor = paper(),
        topBar = {
            MonoTopBar(menuCrumbs(listOf("get", "sms")) + Crumb("show", Route.SmsShow) + Crumb(vm.id.toString()), onCrumb = onCrumb, onBack = onBack) {
                MonoIconButton(
                    Icons.Filled.Refresh,
                    stringResource(R.string.refresh),
                    onClick = vm::load,
                    enabled = state.message !is Load.Loading,
                )
            }
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when (val current = state.message) {
                Load.Loading -> LoadingBox(stringResource(R.string.fetching, stringResource(R.string.message_title)))
                is Load.Error -> ErrorBox(current.message.string(), onRetry = vm::load)
                is Load.Ready -> {
                    val message = current.value
                    KeyValueTable(
                        stringResource(R.string.message_title), listOf(
                            stringResource(R.string.message_id) to message.id.toString(),
                            stringResource(R.string.message_number) to message.number,
                            stringResource(R.string.message_status) to message.tag.label.string(),
                            stringResource(R.string.message_date) to message.date.display(),
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    MonoBox(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.message_content).uppercase(), style = MaterialTheme.typography.labelLarge, color = ink())
                        Spacer(Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                message.content.trim().ifEmpty { stringResource(R.string.dash) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = ink(),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    ButtonRow {
                        MonoButton(stringResource(R.string.reply), { onNavigate(Route.SendSms(message.number)) }, Modifier.weight(1f), primary = true)
                        MonoButton(
                            stringResource(R.string.mark_read),
                            vm::markRead,
                            Modifier.weight(1f),
                            enabled = !state.busy && message.tag is MessageStatus.Unread,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    MonoButton(stringResource(R.string.delete), { confirmDelete = true }, Modifier.fillMaxWidth(), enabled = !state.busy)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        MonoDialog(
            title = stringResource(R.string.delete_message_title),
            message = stringResource(R.string.delete_message_body, vm.id),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                vm.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun SmsShowScreen(onNavigate: (Route) -> Unit, onCrumb: (Route) -> Unit, onBack: () -> Unit) {
    var id by rememberSaveable { mutableStateOf("") }
    val parsed = id.trim().toIntOrNull()

    Scaffold(
        containerColor = paper(),
        topBar = { MonoTopBar(leafCrumbs(listOf("get", "sms", "show")), onCrumb = onCrumb, onBack = onBack) },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
        ) {
            MonoTextField(
                value = id,
                onValueChange = { id = it },
                label = stringResource(R.string.message_id_label),
                placeholder = stringResource(R.string.message_id_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(Modifier.height(16.dp))
            MonoButton(
                stringResource(R.string.show),
                { parsed?.let { onNavigate(Route.SmsMessage(it)) } },
                Modifier.fillMaxWidth(),
                primary = true,
                enabled = parsed != null,
            )
            Spacer(Modifier.height(8.dp))
            MonoButton(stringResource(R.string.browse_inbox), { onNavigate(Route.SmsInbox) }, Modifier.fillMaxWidth())
        }
    }
}

data class SmsSelectUiState(val busy: Boolean = false, val result: Load<ActionResult?>? = null)

class SmsSelectViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: RouterRepository,
) : ViewModel() {
    val action: SmsAction = savedStateHandle.toRoute<Route.SmsSelect>().action

    private val state = MutableStateFlow(SmsSelectUiState())
    val uiState: StateFlow<SmsSelectUiState> = state.asStateFlow()

    fun run(selector: MsgSelector) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, result = Load.Loading) }
        viewModelScope.launch {
            val result = load {
                repository.session {
                    when (action) {
                        SmsAction.Delete -> when (selector) {
                            MsgSelector.All -> deleteAllSms()
                            is MsgSelector.Id -> deleteSms(listOf(selector.id))
                        }

                        SmsAction.Mark -> when (selector) {
                            MsgSelector.All -> markAllSms()
                            is MsgSelector.Id -> markSms(listOf(selector.id))
                        }
                    }
                }
            }
            if (result is Load.Ready) repository.notifyInboxChanged()
            state.update { it.copy(busy = false, result = result) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SmsSelectViewModel(createSavedStateHandle(), container.routerRepository) }
        }
    }
}

@Composable
fun SmsSelectScreen(onCrumb: (Route) -> Unit, onBack: () -> Unit, vm: SmsSelectViewModel = viewModel(factory = SmsSelectViewModel.Factory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var selector by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val parsed = MsgSelector.parse(selector)
    val action = vm.action
    val emptyNotice = when (action) {
        SmsAction.Delete -> R.string.inbox_empty
        SmsAction.Mark -> R.string.no_unread
    }

    Scaffold(
        containerColor = paper(),
        topBar = { MonoTopBar(leafCrumbs(listOf("post", "sms", action.command)), onCrumb = onCrumb, onBack = onBack) },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
        ) {
            MonoTextField(
                value = selector,
                onValueChange = { selector = it },
                label = stringResource(R.string.selector_label),
                placeholder = stringResource(R.string.selector_placeholder),
                enabled = !state.busy,
            )
            Spacer(Modifier.height(16.dp))
            MonoButton(
                stringResource(action.label),
                {
                    val target = parsed ?: return@MonoButton
                    if (action == SmsAction.Delete) confirm = true else vm.run(target)
                },
                Modifier.fillMaxWidth(),
                primary = true,
                enabled = !state.busy && parsed != null,
            )
            Spacer(Modifier.height(16.dp))

            when (val result = state.result) {
                null -> Unit
                Load.Loading -> LoadingBox(stringResource(R.string.working))
                is Load.Error -> ErrorBox(result.message.string())
                is Load.Ready -> {
                    val value = result.value
                    if (value == null) {
                        MonoBox(Modifier.fillMaxWidth()) {
                            Text(stringResource(emptyNotice), style = MaterialTheme.typography.bodyMedium, color = ink())
                        }
                    } else {
                        ReportView(value.table())
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirm) {
        val body = when (parsed) {
            MsgSelector.All -> stringResource(R.string.delete_all_body)
            is MsgSelector.Id -> stringResource(R.string.delete_message_body, parsed.id)
            null -> ""
        }
        MonoDialog(
            title = stringResource(R.string.delete),
            message = body,
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                confirm = false
                parsed?.let(vm::run)
            },
            onDismiss = { confirm = false },
        )
    }
}

data class SendSmsUiState(
    val number: String = "",
    val message: String = "",
    val sending: Boolean = false,
    val result: Load<ActionResult>? = null,
)

class SendSmsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: RouterRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val state = MutableStateFlow(SendSmsUiState(number = savedStateHandle.toRoute<Route.SendSms>().number))
    val uiState: StateFlow<SendSmsUiState> = state.asStateFlow()

    init {
        if (state.value.number.isEmpty()) {
            viewModelScope.launch {
                val remembered = settings.lastRecipient.first()
                state.update { if (it.number.isEmpty()) it.copy(number = remembered) else it }
            }
        }
    }

    fun onNumberChange(value: String) = state.update { it.copy(number = value) }
    fun onMessageChange(value: String) = state.update { it.copy(message = value) }

    fun send() {
        val current = state.value
        val to = current.number.trim()
        val body = current.message
        if (current.sending || to.isEmpty() || body.isEmpty()) return
        state.update { it.copy(sending = true, result = Load.Loading) }
        viewModelScope.launch {
            val result = load { repository.session { sendSms(to, body) } }
            if (result is Load.Ready) {
                repository.notifyInboxChanged()
                settings.rememberRecipient(to)
            }
            state.update { it.copy(sending = false, result = result, message = if (result is Load.Ready) "" else it.message) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SendSmsViewModel(createSavedStateHandle(), container.routerRepository, container.settingsRepository) }
        }
    }
}

@Composable
fun SendSmsScreen(onCrumb: (Route) -> Unit, onBack: () -> Unit, vm: SendSmsViewModel = viewModel(factory = SendSmsViewModel.Factory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = paper(),
        topBar = { MonoTopBar(leafCrumbs(listOf("post", "sms", "send")), onCrumb = onCrumb, onBack = onBack) },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
        ) {
            MonoTextField(
                value = state.number,
                onValueChange = vm::onNumberChange,
                label = stringResource(R.string.recipient_number),
                placeholder = stringResource(R.string.recipient_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = !state.sending,
            )
            Spacer(Modifier.height(16.dp))
            MonoTextField(
                value = state.message,
                onValueChange = vm::onMessageChange,
                label = stringResource(R.string.message),
                singleLine = false,
                minLines = 5,
                enabled = !state.sending,
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    pluralStringResource(R.plurals.chars_count, state.message.length, state.message.length),
                    style = MaterialTheme.typography.labelSmall,
                    color = ink(),
                )
            }
            Spacer(Modifier.height(16.dp))
            MonoButton(
                stringResource(if (state.sending) R.string.sending else R.string.send),
                vm::send,
                Modifier.fillMaxWidth(),
                primary = true,
                enabled = !state.sending && state.number.isNotBlank() && state.message.isNotEmpty(),
            )
            Spacer(Modifier.height(16.dp))

            when (val result = state.result) {
                null -> Unit
                Load.Loading -> LoadingBox(stringResource(R.string.sending).trimEnd('…'))
                is Load.Error -> ErrorBox(result.message.string())
                is Load.Ready -> ReportView(result.value.table())
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
