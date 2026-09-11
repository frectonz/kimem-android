package et.frectonz.kimem.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import et.frectonz.kimem.R
import et.frectonz.kimem.core.data.NoticeCenter
import et.frectonz.kimem.core.data.SettingsRepository
import et.frectonz.kimem.core.model.Settings
import et.frectonz.kimem.core.model.ThemeMode
import et.frectonz.kimem.core.model.res
import et.frectonz.kimem.di.container
import et.frectonz.kimem.ui.components.LoadingBox
import et.frectonz.kimem.ui.components.MonoButton
import et.frectonz.kimem.ui.components.MonoSegmented
import et.frectonz.kimem.ui.components.MonoTextField
import et.frectonz.kimem.ui.components.MonoTopBar
import et.frectonz.kimem.ui.components.SectionHeader
import et.frectonz.kimem.ui.components.ink
import et.frectonz.kimem.ui.components.paper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val notices: NoticeCenter,
) : ViewModel() {
    private val state = MutableStateFlow<Settings?>(null)
    val form: StateFlow<Settings?> = state.asStateFlow()

    init {
        viewModelScope.launch { state.value = repository.settings.first() }
    }

    fun update(transform: (Settings) -> Settings) {
        state.value = state.value?.let(transform)
    }

    fun reset() {
        state.value = Settings()
    }

    fun save(onSaved: () -> Unit) {
        val current = state.value ?: return
        viewModelScope.launch {
            repository.save(current.copy(router = current.router.trim().ifEmpty { Settings().router }))
            notices.post(res(R.string.settings_saved))
            onSaved()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container.settingsRepository, container.notices) }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val form by vm.form.collectAsStateWithLifecycle()
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = paper(),
        topBar = { MonoTopBar(stringResource(R.string.breadcrumb, stringResource(R.string.settings_title)), onBack = onBack) },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
        ) {
            val current = form
            if (current == null) {
                LoadingBox(stringResource(R.string.settings))
                return@Column
            }

            SectionHeader(stringResource(R.string.router_section), Modifier.padding(top = 0.dp))
            MonoTextField(
                value = current.router,
                onValueChange = { value -> vm.update { it.copy(router = value) } },
                label = stringResource(R.string.address),
                placeholder = stringResource(R.string.address_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            Spacer(Modifier.height(16.dp))
            MonoTextField(
                value = current.username,
                onValueChange = { value -> vm.update { it.copy(username = value) } },
                label = stringResource(R.string.username),
            )
            Spacer(Modifier.height(16.dp))
            MonoTextField(
                value = current.password,
                onValueChange = { value -> vm.update { it.copy(password = value) } },
                label = stringResource(R.string.password),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    MonoButton(
                        stringResource(if (showPassword) R.string.hide_password else R.string.show_password),
                        { showPassword = !showPassword },
                        compact = true,
                    )
                },
            )

            SectionHeader(stringResource(R.string.appearance))
            MonoSegmented(
                options = ThemeMode.entries.map { stringResource(it.labelRes) },
                selected = ThemeMode.entries.indexOf(current.theme),
                onSelect = { index -> vm.update { it.copy(theme = ThemeMode.entries[index]) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.theme_help), style = MaterialTheme.typography.bodySmall, color = ink())

            Spacer(Modifier.height(32.dp))
            MonoButton(
                stringResource(R.string.save),
                onClick = { vm.save(onBack) },
                Modifier.fillMaxWidth(),
                primary = true,
                enabled = current.router.isNotBlank(),
            )
            Spacer(Modifier.height(8.dp))
            MonoButton(stringResource(R.string.reset_defaults), vm::reset, Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
}
