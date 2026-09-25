package com.expensetracker.app.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.AppContainer
import com.expensetracker.app.AppLanguage
import com.expensetracker.app.LanguageManager
import com.expensetracker.app.R
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.app.ui.displayName
import com.expensetracker.app.ui.components.AppCard
import com.expensetracker.app.ui.components.SectionLabel
import com.expensetracker.app.ui.onboarding.HowToVerifyDialog
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, onOpenExport: () -> Unit, onOpenRules: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val autoTracking by container.prefs.autoTracking.collectAsStateWithLifecycle()
    val theme by container.prefs.themeMode.collectAsStateWithLifecycle()
    val importState by container.importer.state.collectAsStateWithLifecycle()
    val rules by container.repository.observeRules().collectAsStateWithLifecycle(emptyList())
    var hasPermission by remember { mutableStateOf(container.smsReader.hasPermission()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showHowTo by remember { mutableStateOf(false) }
    val trackingOn = autoTracking && hasPermission
    val language by LanguageManager.language(context).collectAsStateWithLifecycle()
    var showLanguagePicker by remember { mutableStateOf(false) }
    val smsDeniedMessage = stringResource(R.string.set_sms_denied)
    val openLabel = stringResource(R.string.set_open)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasPermission = result[Manifest.permission.READ_SMS] == true
        if (hasPermission) {
            container.prefs.setAutoTracking(true)
            container.importer.start(fromScratch = container.prefs.lastImportedSmsDate == 0L)
        } else {
            scope.launch {
                val r = snackbar.showSnackbar(
                    smsDeniedMessage,
                    actionLabel = openLabel,
                    duration = SnackbarDuration.Long,
                )
                if (r == SnackbarResult.ActionPerformed) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.set_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))

            PrivacyCenter(trackingOn = trackingOn, onHowTo = { showHowTo = true })
            Spacer(Modifier.height(16.dp))

            AppCard(padding = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    NavRow(stringResource(R.string.set_export_title), stringResource(R.string.set_export_subtitle), onOpenExport)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    NavRow(
                        stringResource(R.string.set_rules_title),
                        if (rules.isEmpty()) stringResource(R.string.set_rules_none)
                        else pluralStringResource(R.plurals.set_rules_count, rules.size, rules.size),
                        onOpenRules,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.set_auto_tracking), fontWeight = FontWeight.SemiBold)
                            Text(
                                if (trackingOn) stringResource(R.string.set_auto_on) else stringResource(R.string.set_auto_off),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = trackingOn,
                            onCheckedChange = { on ->
                                if (!on) container.prefs.setAutoTracking(false)
                                else if (hasPermission) {
                                    container.prefs.setAutoTracking(true)
                                    container.importer.start()
                                } else {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                                }
                            },
                        )
                    }
                    if (trackingOn) {
                        if (importState.running) {
                            LinearProgressIndicator(progress = { importState.progress }, modifier = Modifier.fillMaxWidth())
                            Text(
                                stringResource(R.string.set_checking_messages, importState.found),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        } else {
                            TextButton(onClick = { container.importer.start(fromScratch = true) }, modifier = Modifier.padding(bottom = 4.dp)) {
                                Text(stringResource(R.string.set_scan_again))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.set_appearance))
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = theme == mode,
                        onClick = { container.prefs.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                    ) { Text(mode.displayName()) }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.set_language))
            Spacer(Modifier.height(8.dp))
            // A row plus a picker, not segmented buttons: "फ़ोन की भाषा" doesn't fit in a three-way row.
            AppCard(padding = 0.dp) {
                NavRow(stringResource(language.labelRes), stringResource(R.string.set_language_change)) { showLanguagePicker = true }
            }

            Spacer(Modifier.height(28.dp))
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text(stringResource(R.string.set_delete_all), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(32.dp))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (showHowTo) HowToVerifyDialog(onDismiss = { showHowTo = false })

    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(stringResource(R.string.set_language)) },
            text = {
                Column {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(selected = lang == language, role = Role.RadioButton) {
                                    showLanguagePicker = false
                                    if (lang != language) LanguageManager.set(context, lang)
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                        ) {
                            RadioButton(selected = lang == language, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(lang.labelRes), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLanguagePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.set_delete_title)) },
            text = { Text(stringResource(R.string.set_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        container.repository.deleteEverything()
                        container.prefs.clear() // back to onboarding
                    }
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun PrivacyCenter(trackingOn: Boolean, onHowTo: () -> Unit) {
    val onCard = MaterialTheme.colorScheme.onPrimary
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = onCard)
            Text(stringResource(R.string.set_privacy_center), color = onCard, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Spacer(Modifier.height(12.dp))
        listOf(
            stringResource(R.string.set_privacy_offline) to stringResource(R.string.set_privacy_always),
            stringResource(R.string.set_privacy_auto_tracking) to
                stringResource(if (trackingOn) R.string.set_privacy_on else R.string.set_privacy_off),
            stringResource(R.string.set_privacy_backup) to stringResource(R.string.set_privacy_off),
            stringResource(R.string.set_privacy_stored) to stringResource(R.string.set_privacy_this_phone),
        ).forEach { (k, v) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(k, color = onCard.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
                Text(v, color = onCard, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = onCard.copy(alpha = 0.2f))
        Text(
            stringResource(R.string.set_privacy_no_internet),
            color = onCard.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            stringResource(R.string.set_privacy_how_to),
            color = onCard,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable(onClick = onHowTo).padding(top = 8.dp),
        )
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

