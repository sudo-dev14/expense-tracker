package com.expensetracker.app.ui.onboarding

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.AppContainer
import com.expensetracker.app.R
import com.expensetracker.app.data.accountLabel
import com.expensetracker.app.data.categoryEnum
import com.expensetracker.app.ui.components.AppCard
import com.expensetracker.app.ui.merchantName
import com.expensetracker.app.ui.components.FULL_DATE
import com.expensetracker.app.ui.components.MerchantAvatar
import com.expensetracker.app.ui.components.SectionLabel
import com.expensetracker.app.ui.components.toLocalDate
import com.expensetracker.core.format.Money
import com.expensetracker.core.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

private enum class Step { PRIVACY, AUTO_TRACKING, IMPORT }

@Composable
fun OnboardingFlow(container: AppContainer, onFinished: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(Step.PRIVACY) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.READ_SMS] == true) {
            container.prefs.setAutoTracking(true)
            container.importer.start(fromScratch = true)
            step = Step.IMPORT
        } else {
            // Works fine without it: the user adds expenses manually and can turn tracking on later.
            container.prefs.setAutoTracking(false)
            onFinished()
        }
    }

    BackHandler(enabled = step != Step.PRIVACY) {
        step = if (step == Step.IMPORT) Step.AUTO_TRACKING else Step.PRIVACY
    }

    when (step) {
        Step.PRIVACY -> PrivacyPromise(onContinue = { step = Step.AUTO_TRACKING })
        Step.AUTO_TRACKING -> AutoTracking(
            onBack = { step = Step.PRIVACY },
            onTurnOn = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)) },
            onSkip = {
                container.prefs.setAutoTracking(false)
                onFinished()
            },
        )
        Step.IMPORT -> ImportProgress(container, onContinue = onFinished)
    }
}

@Composable
private fun OnboardingPage(
    stepLabel: String,
    onBack: (() -> Unit)? = null,
    bottom: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            } else {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge.copy(fontFamily = com.expensetracker.app.ui.theme.DisplayFamily, fontWeight = FontWeight.SemiBold))
            }
            Spacer(Modifier.weight(1f))
            Text(stepLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { content() }
        bottom()
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

@Composable
private fun PrivacyPromise(onContinue: () -> Unit) {
    var showHowTo by remember { mutableStateOf(false) }
    OnboardingPage(
        stepLabel = stringResource(R.string.onb_step_of, 1, 3),
        bottom = { PrimaryButton(stringResource(R.string.onb_continue), onContinue) },
    ) {
        Spacer(Modifier.height(40.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.onb_privacy_title), style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp, lineHeight = 38.sp))
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.onb_privacy_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            PromisePoint(stringResource(R.string.onb_promise_internet_title), stringResource(R.string.onb_promise_internet_body))
            PromisePoint(stringResource(R.string.onb_promise_account_title), stringResource(R.string.onb_promise_account_body))
            PromisePoint(stringResource(R.string.onb_promise_ads_title), stringResource(R.string.onb_promise_ads_body))
        }
        TextButton(onClick = { showHowTo = true }, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.onb_how_to_check), fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)
        }
    }
    if (showHowTo) HowToVerifyDialog(onDismiss = { showHowTo = false })
}

@Composable
fun HowToVerifyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.onb_verify_got_it)) } },
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.onb_verify_title)) },
        text = {
            Text(stringResource(R.string.onb_verify_body))
        },
    )
}

@Composable
private fun PromisePoint(title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AutoTracking(onBack: () -> Unit, onTurnOn: () -> Unit, onSkip: () -> Unit) {
    OnboardingPage(
        stepLabel = stringResource(R.string.onb_step_of, 2, 3),
        onBack = onBack,
        bottom = {
            PrimaryButton(stringResource(R.string.onb_auto_turn_on), onTurnOn)
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(stringResource(R.string.onb_auto_skip), fontWeight = FontWeight.SemiBold)
            }
        },
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.onb_auto_title), style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp, lineHeight = 37.sp))
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.onb_auto_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        AppCard(padding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Quality(Icons.Outlined.WifiOff, stringResource(R.string.onb_quality_offline_title), stringResource(R.string.onb_quality_offline_body))
                Quality(Icons.Outlined.PhoneAndroid, stringResource(R.string.onb_quality_local_title), stringResource(R.string.onb_quality_local_body))
                Quality(Icons.Outlined.DoneAll, stringResource(R.string.onb_quality_spending_title), stringResource(R.string.onb_quality_spending_body))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.onb_auto_switch_off_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Quality(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ImportProgress(container: AppContainer, onContinue: () -> Unit) {
    val state by container.importer.state.collectAsStateWithLifecycle()
    val numbers = remember { NumberFormat.getIntegerInstance(Locale("en", "IN")) }
    OnboardingPage(
        stepLabel = stringResource(R.string.onb_step_of, 3, 3),
        bottom = {
            if (state.finished) {
                PrimaryButton(stringResource(R.string.onb_import_see_spending), onContinue)
            } else {
                OutlinedButton(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) { Text(stringResource(R.string.onb_import_continue_background), fontWeight = FontWeight.Bold) }
            }
        },
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            if (state.finished) stringResource(R.string.onb_import_done_title) else stringResource(R.string.onb_import_running_title),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp),
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.onb_import_offline_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row {
            Text(stringResource(R.string.onb_import_percent, (state.progress * 100).toInt()), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            state.oldestDate?.let {
                Text(stringResource(R.string.onb_import_date_range, it.toLocalDate().format(FULL_DATE)), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(numbers.format(state.scanned), pluralStringResource(R.plurals.onb_import_messages_checked, state.scanned), Modifier.weight(1f))
            StatTile(numbers.format(state.found), pluralStringResource(R.plurals.onb_import_transactions_found, state.found), Modifier.weight(1f), highlight = true)
        }
        if (state.recent.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.onb_import_just_found))
            Spacer(Modifier.height(10.dp))
            AppCard(padding = 0.dp) {
                Column {
                    state.recent.forEach { tx ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            MerchantAvatar(tx.merchantName(), tx.categoryEnum, size = 36.dp)
                            Column(Modifier.weight(1f)) {
                                Text(tx.merchantName(), fontWeight = FontWeight.SemiBold)
                                Text(
                                    listOfNotNull(tx.accountLabel, tx.timestamp.toLocalDate().format(FULL_DATE)).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            val credit = tx.type == TransactionType.CREDIT
                            Text(Money.format(if (credit) tx.amountMinor else -tx.amountMinor, signed = true), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier, highlight: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
