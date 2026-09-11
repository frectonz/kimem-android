package et.frectonz.kimem.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import et.frectonz.kimem.core.model.Report
import et.frectonz.kimem.core.model.Show
import et.frectonz.kimem.core.model.res
import et.frectonz.kimem.di.container
import et.frectonz.kimem.ui.Load
import et.frectonz.kimem.ui.Route
import et.frectonz.kimem.ui.components.ErrorBox
import et.frectonz.kimem.ui.components.KeyValueTable
import et.frectonz.kimem.ui.components.LoadingBox
import et.frectonz.kimem.ui.components.MonoBox
import et.frectonz.kimem.ui.components.MonoButton
import et.frectonz.kimem.ui.components.MonoIconButton
import et.frectonz.kimem.ui.components.MonoTopBar
import et.frectonz.kimem.ui.components.ink
import et.frectonz.kimem.ui.components.paper
import et.frectonz.kimem.ui.fetchReport
import et.frectonz.kimem.ui.load
import et.frectonz.kimem.ui.string
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: RouterRepository,
    private val notices: NoticeCenter,
) : ViewModel() {
    val kind = savedStateHandle.toRoute<Route.Report>().kind

    private val state = MutableStateFlow<Load<Show>>(Load.Loading)
    val uiState: StateFlow<Load<Show>> = state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            state.value = Load.Loading
            state.value = load { repository.session { fetchReport(kind) } }
        }
    }

    fun copied() {
        notices.post(res(R.string.copied))
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ReportViewModel(createSavedStateHandle(), container.routerRepository, container.notices) }
        }
    }
}

@Composable
fun ReportScreen(onBack: () -> Unit, vm: ReportViewModel = viewModel(factory = ReportViewModel.Factory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val kind = vm.kind

    Scaffold(
        containerColor = paper(),
        topBar = {
            MonoTopBar(stringResource(R.string.breadcrumb, kind.command), onBack = onBack) {
                MonoIconButton(
                    Icons.Filled.Refresh,
                    stringResource(R.string.refresh),
                    onClick = vm::load,
                    enabled = state !is Load.Loading,
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
            when (val current = state) {
                Load.Loading -> LoadingBox(stringResource(R.string.fetching, stringResource(kind.title)))
                is Load.Error -> ErrorBox(current.message.string(), onRetry = vm::load)
                is Load.Ready -> ReportView(current.value.table(), onCopied = vm::copied)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ReportView(report: Report, onCopied: () -> Unit = {}) {
    when (report) {
        is Report.KeyValues -> KeyValueTable(
            report.title.string(),
            report.rows.map { (k, v) -> k.string() to v.string() },
        )

        is Report.Text -> CodeBlock(report.title.string(), report.text, onCopied)

        is Report.Records -> {
            if (report.rows.isEmpty()) {
                MonoBox(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.no_entries), style = MaterialTheme.typography.bodyMedium, color = ink())
                }
            }
            val columns = report.columns.map { it.string() }
            report.rows.forEachIndexed { index, row ->
                if (index > 0) Spacer(Modifier.height(12.dp))
                KeyValueTable(
                    stringResource(R.string.record_title, report.title.string(), index + 1),
                    columns.zip(row.map { it.string() }),
                )
            }
        }
    }
}

@Composable
fun CodeBlock(title: String, text: String, onCopied: () -> Unit) {
    val context = LocalContext.current

    MonoBox(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = ink(), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            MonoButton(stringResource(R.string.copy), compact = true, onClick = {
                copyToClipboard(context, text)
                onCopied()
            })
        }
        Spacer(Modifier.height(12.dp))
        SelectionContainer {
            Text(
                text.ifEmpty { stringResource(R.string.dash) },
                style = MaterialTheme.typography.bodySmall,
                color = ink(),
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("kimem", text))
}
