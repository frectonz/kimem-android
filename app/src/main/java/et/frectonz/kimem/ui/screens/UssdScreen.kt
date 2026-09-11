package et.frectonz.kimem.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
import et.frectonz.kimem.core.data.RouterRepository
import et.frectonz.kimem.core.model.UiText
import et.frectonz.kimem.core.model.USSD_CODES
import et.frectonz.kimem.core.model.res
import et.frectonz.kimem.core.model.resolveUssdCode
import et.frectonz.kimem.core.model.ussdBuiltinsHelp
import et.frectonz.kimem.core.network.Router
import et.frectonz.kimem.di.container
import et.frectonz.kimem.ui.Route
import et.frectonz.kimem.ui.leafCrumbs
import et.frectonz.kimem.ui.components.ErrorBox
import et.frectonz.kimem.ui.components.LoadingBox
import et.frectonz.kimem.ui.components.MonoBox
import et.frectonz.kimem.ui.components.MonoButton
import et.frectonz.kimem.ui.components.MonoDivider
import et.frectonz.kimem.ui.components.MonoListItem
import et.frectonz.kimem.ui.components.MonoTag
import et.frectonz.kimem.ui.components.MonoTextField
import et.frectonz.kimem.ui.components.MonoTopBar
import et.frectonz.kimem.ui.components.SectionHeader
import et.frectonz.kimem.ui.components.ink
import et.frectonz.kimem.ui.components.paper
import et.frectonz.kimem.ui.string
import et.frectonz.kimem.ui.uiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UssdEntry {
    data class Dialed(val code: String) : UssdEntry
    data class Response(val text: String) : UssdEntry
    data class Reply(val text: String) : UssdEntry
    data class Error(val text: UiText) : UssdEntry
}

enum class UssdPhase { Idle, Active, Ended }

data class UssdUiState(
    val phase: UssdPhase = UssdPhase.Idle,
    val transcript: List<UssdEntry> = emptyList(),
    val busy: Boolean = false,
    val formError: UiText? = null,
)

class UssdViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: RouterRepository,
    private val cleanupScope: CoroutineScope,
) : ViewModel() {
    val initialCode: String = savedStateHandle.toRoute<Route.Ussd>().code

    private val state = MutableStateFlow(UssdUiState())
    val uiState: StateFlow<UssdUiState> = state.asStateFlow()

    private var router: Router? = null
    private var job: Job? = null

    init {
        if (initialCode.isNotEmpty()) dial(initialCode)
    }

    fun dial(raw: String) {
        if (state.value.busy) return
        val resolved = resolveUssdCode(raw)
        if (resolved == null) {
            state.update { it.copy(formError = res(R.string.ussd_invalid, raw.trim(), ussdBuiltinsHelp())) }
            return
        }

        state.update { it.copy(formError = null, phase = UssdPhase.Active, transcript = listOf(UssdEntry.Dialed(resolved))) }
        run {
            val session = repository.open()
            session.login()
            router = session
            session.ussdSend(resolved)
            append(UssdEntry.Response(session.ussdAwaitResponse()))
        }
    }

    fun send(reply: String) {
        val text = reply.trim()
        val session = router
        if (state.value.busy || text.isEmpty() || session == null) return
        append(UssdEntry.Reply(text))
        run {
            session.ussdReply(text)
            append(UssdEntry.Response(session.ussdAwaitResponse()))
        }
    }

    fun end() {
        if (state.value.phase == UssdPhase.Active) finish()
    }

    fun reset() {
        end()
        state.update { it.copy(phase = UssdPhase.Idle, transcript = emptyList()) }
    }

    private fun append(entry: UssdEntry) {
        state.update { it.copy(transcript = it.transcript + entry) }
    }

    private fun run(block: suspend () -> Unit) {
        state.update { it.copy(busy = true) }
        job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                append(UssdEntry.Error(e.uiText()))
                finish()
            } finally {
                state.update { it.copy(busy = false) }
            }
        }
    }

    private fun finish() {
        state.update { it.copy(phase = UssdPhase.Ended) }
        job?.cancel()
        job = null
        val session = router ?: return
        router = null
        cleanupScope.launch {
            session.ussdCancel()
            try {
                session.logout()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    override fun onCleared() {
        finish()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { UssdViewModel(createSavedStateHandle(), container.routerRepository, container.applicationScope) }
        }
    }
}

@Composable
fun UssdScreen(onCrumb: (Route) -> Unit, onBack: () -> Unit, vm: UssdViewModel = viewModel(factory = UssdViewModel.Factory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = paper(),
        topBar = {
            MonoTopBar(leafCrumbs(listOf("post", "ussd")), onCrumb = onCrumb, onBack = onBack) {
                when (state.phase) {
                    UssdPhase.Idle -> Unit
                    UssdPhase.Active -> MonoButton(stringResource(R.string.end), vm::end, compact = true)
                    UssdPhase.Ended -> MonoButton(stringResource(R.string.new_short), vm::reset, compact = true)
                }
            }
        },
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
        ) {
            when (state.phase) {
                UssdPhase.Idle -> DialForm(state, onDial = vm::dial)
                else -> Transcript(state, onSend = vm::send, onReset = vm::reset)
            }
        }
    }
}

@Composable
private fun DialForm(state: UssdUiState, onDial: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        MonoTextField(
            value = code,
            onValueChange = { code = it },
            label = stringResource(R.string.ussd_code_label),
            placeholder = stringResource(R.string.ussd_placeholder),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Go),
        )
        Spacer(Modifier.height(16.dp))
        MonoButton(stringResource(R.string.dial), { onDial(code) }, Modifier.fillMaxWidth(), primary = true, enabled = code.isNotBlank())

        state.formError?.let {
            Spacer(Modifier.height(16.dp))
            ErrorBox(it.string())
        }

        SectionHeader(stringResource(R.string.builtins))
        val ink = ink()
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, ink)
        ) {
            USSD_CODES.forEachIndexed { index, (name, dialCode) ->
                if (index > 0) MonoDivider()
                MonoListItem(title = name, subtitle = dialCode, onClick = { onDial(dialCode) })
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Transcript(state: UssdUiState, onSend: (String) -> Unit, onReset: () -> Unit) {
    val listState = rememberLazyListState()
    var reply by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.transcript.size, state.busy) {
        val count = state.transcript.size + if (state.busy) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.transcript) { entry -> TranscriptEntry(entry) }
            if (state.busy) item { LoadingBox(stringResource(R.string.waiting_for_network)) }
            if (state.phase == UssdPhase.Ended) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        MonoTag(stringResource(R.string.session_ended))
                    }
                }
            }
        }

        MonoDivider()
        if (state.phase == UssdPhase.Active) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                MonoTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = stringResource(R.string.reply_label),
                    modifier = Modifier.weight(1f),
                    enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Send),
                )
                Spacer(Modifier.width(8.dp))
                MonoButton(
                    stringResource(R.string.send),
                    {
                        onSend(reply)
                        reply = ""
                    },
                    primary = true,
                    enabled = !state.busy && reply.isNotBlank(),
                )
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                MonoButton(stringResource(R.string.new_session), onReset, Modifier.fillMaxWidth(), primary = true)
            }
        }
    }
}

@Composable
private fun TranscriptEntry(entry: UssdEntry) {
    when (entry) {
        is UssdEntry.Dialed -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            MonoTag(stringResource(R.string.dial_tag, entry.code), inverted = true)
        }

        is UssdEntry.Response -> MonoBox(Modifier.fillMaxWidth()) {
            SelectionContainer {
                Text(entry.text.ifEmpty { stringResource(R.string.dash) }, style = MaterialTheme.typography.bodyMedium, color = ink())
            }
        }

        is UssdEntry.Reply -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("> ${entry.text}", style = MaterialTheme.typography.bodyLarge, color = ink())
        }

        is UssdEntry.Error -> ErrorBox(entry.text.string())
    }
}
