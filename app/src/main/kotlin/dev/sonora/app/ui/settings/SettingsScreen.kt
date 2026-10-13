package dev.sonora.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sonora.app.R
import dev.sonora.app.data.ThemeMode
import dev.sonora.app.ui.components.DisclosureCard
import dev.sonora.core.domain.ExternalLinks
import kotlinx.coroutines.delay

/**
 * Settings: appearance, motion, locally stored data, the optional YouTube
 * provider and the privacy/attribution information required for a music app
 * that talks to third party APIs.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.historyCleared) {
        if (state.historyCleared) {
            delay(2_500)
            viewModel.acknowledgeHistoryCleared()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        SettingsSection(title = stringResource(R.string.settings_appearance)) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(text = stringResource(themeLabel(mode))) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_motion),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_motion_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.reduceMotion,
                    onCheckedChange = viewModel::setReduceMotion,
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_data)) {
            SettingsValueRow(
                label = stringResource(R.string.settings_favorites_count),
                value = state.favoritesCount.toString(),
            )
            SettingsValueRow(
                label = stringResource(R.string.settings_history_count),
                value = state.historyCount.toString(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = viewModel::clearHistory) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_clear_history),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                if (state.historyCleared) {
                    Text(
                        text = stringResource(R.string.settings_history_cleared),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        SettingsSection(title = stringResource(R.string.settings_youtube)) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        text = if (state.youTubeAvailability.isAvailable) {
                            stringResource(R.string.settings_youtube_enabled)
                        } else {
                            stringResource(R.string.settings_youtube_missing)
                        },
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
            Text(
                text = stringResource(R.string.settings_youtube_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DisclosureCard(
                title = stringResource(R.string.attribution_youtube),
                message = stringResource(R.string.settings_youtube_policy),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onOpenUrl(ExternalLinks.YOUTUBE_API_KEY_GUIDE) }) {
                    Text(text = stringResource(R.string.settings_youtube_setup))
                }
                TextButton(onClick = { onOpenUrl(ExternalLinks.YOUTUBE_API_SERVICES_TERMS) }) {
                    Text(text = stringResource(R.string.settings_youtube_terms))
                }
                TextButton(onClick = { onOpenUrl(ExternalLinks.YOUTUBE_DEVELOPER_POLICIES) }) {
                    Text(text = stringResource(R.string.settings_youtube_policies))
                }
            }
        }

        SettingsSection(title = stringResource(R.string.settings_about)) {
            Text(
                text = stringResource(R.string.settings_version, state.versionName),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.settings_about_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DisclosureCard(
                title = stringResource(R.string.settings_privacy),
                message = stringResource(R.string.settings_privacy_message),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onOpenUrl(ExternalLinks.ITUNES_SEARCH_API_DOCS) }) {
                    Text(text = stringResource(R.string.settings_itunes_docs))
                }
                TextButton(onClick = { onOpenUrl(ExternalLinks.GOOGLE_PRIVACY_POLICY) }) {
                    Text(text = stringResource(R.string.youtube_privacy_label))
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        HorizontalDivider()
        content()
    }
}

@Composable
private fun SettingsValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}
