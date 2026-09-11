package et.frectonz.kimem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import et.frectonz.kimem.R

@Composable
fun ink(): Color = MaterialTheme.colorScheme.onBackground

@Composable
fun paper(): Color = MaterialTheme.colorScheme.background

@Composable
fun MonoDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = ink())
}

private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    val stroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
    val inset = 0.5.dp.toPx()
    drawRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = size.copy(width = size.width - 2 * inset, height = size.height - 2 * inset),
        style = stroke,
    )
}

@Composable
fun MonoBox(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .border(1.dp, ink())
            .background(paper())
            .padding(padding),
        content = content,
    )
}

@Composable
fun MonoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val inverted = enabled && (primary xor pressed)
    val ink = ink()
    val paper = paper()

    val frame = if (enabled) Modifier.border(1.dp, ink) else Modifier.dashedBorder(ink)
    val inset = if (compact) PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    else PaddingValues(horizontal = 16.dp, vertical = 12.dp)

    Box(
        modifier
            .background(if (inverted) ink else paper)
            .then(frame)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(inset),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            color = if (inverted) paper else ink,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MonoIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val ink = ink()
    val paper = paper()

    Box(
        modifier
            .size(48.dp)
            .background(if (pressed) ink else paper)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = if (pressed) paper else ink)
    }
}

@Composable
fun MonoSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = ink()
    val paper = paper()

    Row(modifier.border(1.dp, ink).height(IntrinsicSize.Min)) {
        options.forEachIndexed { index, option ->
            if (index > 0) VerticalDivider(Modifier.fillMaxHeight(), thickness = 1.dp, color = ink)
            val active = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (active) ink else paper)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onSelect(index)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.uppercase(),
                    color = if (active) paper else ink,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun MonoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val ink = ink()

    Column(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = ink)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .border(1.dp, ink)
                .background(paper())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                cursorBrush = SolidColor(ink),
                singleLine = singleLine,
                minLines = minLines,
                keyboardOptions = keyboardOptions,
                visualTransformation = visualTransformation,
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                placeholder,
                                color = ink,
                                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                            )
                        }
                        inner()
                    }
                },
            )
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

@Composable
fun MonoTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(paper())
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Spacer(Modifier.width(4.dp))
                MonoIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack)
                Spacer(Modifier.width(12.dp))
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = ink(),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            actions()
            Spacer(Modifier.width(4.dp))
        }
        MonoDivider()
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = ink(),
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun KeyValueTable(
    title: String,
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    val ink = ink()
    val paper = paper()

    SelectionContainer {
        Column(
            modifier
                .fillMaxWidth()
                .border(1.dp, ink)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(ink)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    title.uppercase(),
                    color = paper,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(0.4f),
                )
                Text(
                    stringResource(R.string.value).uppercase(),
                    color = paper,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(0.6f).padding(start = 12.dp),
                )
            }
            rows.forEachIndexed { index, (key, value) ->
                if (index > 0) HorizontalDivider(thickness = 1.dp, color = ink)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    Text(
                        key,
                        style = MaterialTheme.typography.bodySmall,
                        color = ink,
                        modifier = Modifier
                            .weight(0.4f)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                    VerticalDivider(Modifier.fillMaxHeight(), thickness = 1.dp, color = ink)
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink,
                        modifier = Modifier
                            .weight(0.6f)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun MonoTag(text: String, modifier: Modifier = Modifier, inverted: Boolean = false) {
    val ink = ink()
    val paper = paper()
    Box(
        modifier
            .border(1.dp, ink)
            .background(if (inverted) ink else paper)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text.uppercase(),
            color = if (inverted) paper else ink,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun LoadingBox(text: String, modifier: Modifier = Modifier) {
    MonoBox(modifier.fillMaxWidth()) {
        Text("$text…".uppercase(), style = MaterialTheme.typography.labelLarge, color = ink())
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = ink(),
            trackColor = paper(),
            strokeCap = StrokeCap.Butt,
        )
    }
}

@Composable
fun ErrorBox(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    val ink = ink()
    val paper = paper()
    Column(
        modifier
            .fillMaxWidth()
            .border(1.dp, ink)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(ink)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(stringResource(R.string.error).uppercase(), color = paper, style = MaterialTheme.typography.labelLarge)
        }
        SelectionContainer {
            Text(
                message,
                color = ink,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
        }
        if (onRetry != null) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                MonoButton(stringResource(R.string.retry), onRetry)
            }
        }
    }
}

@Composable
fun MonoDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        MonoBox(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.titleMedium, color = ink())
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = ink())
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoButton(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f))
                MonoButton(confirmText, onConfirm, Modifier.weight(1f), primary = true)
            }
        }
    }
}

@Composable
fun MonoSnackbar(data: SnackbarData) {
    val ink = ink()
    val paper = paper()
    Box(
        Modifier
            .fillMaxWidth()
            .background(paper)
            .border(1.dp, ink)
            .padding(3.dp)
            .background(ink)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                data.dismiss()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            data.visuals.message.trimEnd('.').uppercase(),
            color = paper,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun MonoListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val ink = ink()
    val paper = paper()
    val fg = if (pressed) paper else ink

    Row(
        modifier
            .fillMaxWidth()
            .background(if (pressed) ink else paper)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(12.dp))
        if (trailing != null) {
            trailing()
        } else {
            Text("›", style = MaterialTheme.typography.titleLarge, color = fg)
        }
    }
}

@Composable
fun ButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}
