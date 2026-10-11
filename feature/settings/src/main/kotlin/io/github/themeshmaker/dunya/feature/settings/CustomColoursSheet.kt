package io.github.themeshmaker.dunya.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.themeshmaker.dunya.core.designsystem.component.GeometricPattern
import io.github.themeshmaker.dunya.core.designsystem.component.PillChoice
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.designsystem.theme.gradient
import io.github.themeshmaker.dunya.core.model.CustomColors

/** A ready-made palette to start from; the sliders below fine-tune any of them. */
private val Swatches = listOf(
    0xFF0B3D2E, 0xFF0E8A62, 0xFF2BB3A7, 0xFF1D5FAF, 0xFF1D2671, 0xFF4B2C82, 0xFF8E3B8E, 0xFFC2456F,
    0xFFB3261E, 0xFFC2622D, 0xFFE08A1E, 0xFFD4AF37, 0xFF6B8E23, 0xFF5D4037, 0xFF37474F, 0xFF111111
).map { it.toInt() }

/** Hue (0–360), saturation and brightness (0–1). */
private data class Hsv(val h: Float, val s: Float, val v: Float) {
    fun argb(): Int = Color.hsv(h.coerceIn(0f, 359.9f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)).toArgb()

    companion object {
        fun of(argb: Int): Hsv = FloatArray(3).let {
            android.graphics.Color.colorToHSV(argb, it)
            Hsv(it[0], it[1], it[2])
        }
    }
}

/** Pick the two colours of the custom theme's gradient, with a live preview. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomColoursSheet(initial: CustomColors, onApply: (CustomColors) -> Unit, onDismiss: () -> Unit) {
    var start by rememberSaveable { mutableIntStateOf(initial.start) }
    var end by rememberSaveable { mutableIntStateOf(initial.end) }
    var editingEnd by rememberSaveable { mutableStateOf(false) }
    // Kept as HSV while editing, so dragging brightness to black doesn't lose the hue.
    var hsv by remember(editingEnd) { mutableStateOf(Hsv.of(if (editingEnd) end else start)) }
    fun set(value: Hsv) {
        hsv = value
        if (editingEnd) end = value.argb() else start = value.argb()
    }
    val look = DeenTheme.look
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = look.sheetColor, shape = look.sheetShape) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.feature_settings_custom_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.feature_settings_custom_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val gradient = CustomColors(start, end).gradient()
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(look.cardShape)
                    .background(gradient.brush),
                contentAlignment = Alignment.Center
            ) {
                GeometricPattern(color = gradient.pattern, modifier = Modifier.matchParentSize(), cell = 36.dp)
                Text("دِين", style = DeenTextStyles.arabicDisplay.copy(fontSize = 40.sp, lineHeight = 60.sp), color = gradient.content)
            }
            PillChoice(
                options = listOf(false, true),
                selected = editingEnd,
                label = { stringResource(if (it) R.string.feature_settings_custom_second else R.string.feature_settings_custom_first) },
                onSelect = { editingEnd = it },
                modifier = Modifier.fillMaxWidth()
            )
            val current = if (editingEnd) end else start
            Swatches.chunked(8).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { swatch ->
                        val chosen = swatch == current
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .then(if (chosen) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .padding(if (chosen) 4.dp else 0.dp)
                                .clip(CircleShape)
                                .background(Color(swatch))
                                .clickable(role = Role.RadioButton) { set(Hsv.of(swatch)) }
                                .semantics { selected = chosen }
                        )
                    }
                }
            }
            // Colour tracks read left to right in every language.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ColourSlider(
                        label = stringResource(R.string.feature_settings_custom_hue),
                        value = hsv.h / 360f,
                        track = Brush.horizontalGradient((0..6).map { Color.hsv((it * 60f).coerceAtMost(359.9f), 1f, 1f) }),
                        thumb = Color(current)
                    ) { set(hsv.copy(h = it * 360f)) }
                    ColourSlider(
                        label = stringResource(R.string.feature_settings_custom_saturation),
                        value = hsv.s,
                        track = Brush.horizontalGradient(
                            listOf(Color.hsv(hsv.h.coerceIn(0f, 359.9f), 0f, hsv.v), Color.hsv(hsv.h.coerceIn(0f, 359.9f), 1f, hsv.v))
                        ),
                        thumb = Color(current)
                    ) { set(hsv.copy(s = it)) }
                    ColourSlider(
                        label = stringResource(R.string.feature_settings_custom_brightness),
                        value = hsv.v,
                        track = Brush.horizontalGradient(listOf(Color.Black, Color.hsv(hsv.h.coerceIn(0f, 359.9f), hsv.s, 1f))),
                        thumb = Color(current)
                    ) { set(hsv.copy(v = it)) }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                Button(onClick = { onApply(CustomColors(start, end)) }) { Text(stringResource(R.string.feature_settings_custom_apply)) }
            }
        }
    }
}

/** A slider whose track shows the colours it moves through (hue, saturation or brightness). */
@Composable
private fun ColourSlider(label: String, value: Float, track: Brush, thumb: Color, onChange: (Float) -> Unit) {
    val latest by rememberUpdatedState(onChange)
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .semantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                    setProgress { target ->
                        latest(target.coerceIn(0f, 1f))
                        true
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        latest((down.position.x / size.width).coerceIn(0f, 1f))
                        drag(down.id) { change ->
                            latest((change.position.x / size.width).coerceIn(0f, 1f))
                            change.consume()
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(track)
            )
            Box(
                Modifier
                    .offset(x = (maxWidth - 26.dp) * value.coerceIn(0f, 1f))
                    .size(26.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(thumb)
                    .border(3.dp, Color.White, CircleShape)
            )
        }
    }
}
