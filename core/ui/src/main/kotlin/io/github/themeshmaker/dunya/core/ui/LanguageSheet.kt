package io.github.themeshmaker.dunya.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.AppLanguage

/** How a language is shown in pickers: in its own language, or "Phone's language". */
@Composable
fun AppLanguage.displayName(): String = nativeName ?: stringResource(R.string.core_ui_language_system)

/** Every app language, each written in its own script, with the current one ticked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSheet(current: AppLanguage, onPick: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    val look = DeenTheme.look
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = look.sheetColor, shape = look.sheetShape) {
        Text(
            stringResource(R.string.core_ui_language),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
        )
        LazyColumn(Modifier.fillMaxWidth()) {
            items(AppLanguage.entries, key = { it.name }) { language ->
                val chosen = language == current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.RadioButton) { onPick(language) }
                        .semantics { selected = chosen }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        language.displayName(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (chosen) {
                        Icon(
                            painterResource(DeenIcons.Check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
