package com.expensetracker.app.ui.review

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.app.AppContainer
import com.expensetracker.app.data.Status
import com.expensetracker.app.data.TransactionEntity
import com.expensetracker.app.data.accountLabel
import com.expensetracker.app.data.categoryEnum
import com.expensetracker.app.ui.components.AppCard
import com.expensetracker.app.ui.components.SectionLabel
import com.expensetracker.app.ui.components.formatDateTime
import com.expensetracker.core.format.Money
import com.expensetracker.core.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReviewViewModel(private val container: AppContainer) : ViewModel() {
    val items = container.repository.observeNeedsReview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun confirm(tx: TransactionEntity) = viewModelScope.launch { container.repository.setStatus(tx.id, Status.CONFIRMED) }
    fun ignore(tx: TransactionEntity) = viewModelScope.launch { container.repository.setStatus(tx.id, Status.IGNORED) }
}

/** One card at a time: "Looks right", "Edit" or "Ignore". */
@Composable
fun ReviewScreen(container: AppContainer, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val vm: ReviewViewModel = viewModel { ReviewViewModel(container) }
    val items by vm.items.collectAsStateWithLifecycle()
    val list = items.orEmpty()
    val current = list.firstOrNull()

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(56.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text("Needs review", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (list.isNotEmpty()) {
                Text("${list.size} left", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (items == null) {
            // Still loading; show nothing rather than a misleading "all caught up".
        } else if (current == null) {
            AllCaughtUp()
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(8.dp))
                ReviewCard(current)
            }
            ReviewActions(
                onIgnore = { vm.ignore(current) },
                onEdit = { onEdit(current.id) },
                onConfirm = { vm.confirm(current) },
            )
        }
    }
}

@Composable
private fun AllCaughtUp() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("All caught up", style = MaterialTheme.typography.headlineSmall)
        Text("Nothing needs checking right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReviewActions(onIgnore: () -> Unit, onEdit: () -> Unit, onConfirm: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 16.dp)) {
        OutlinedButton(onClick = onIgnore, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(4.dp))
            Text("Ignore")
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(4.dp))
            Text("Edit")
        }
        Button(onClick = onConfirm, modifier = Modifier.weight(1.2f).height(56.dp), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(4.dp))
            Text("Looks right", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReviewCard(tx: TransactionEntity) {
    AppCard(padding = 20.dp) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(14.dp))
                Text(
                    if (tx.merchant == null) "Couldn't tell who was paid" else "Unfamiliar message format",
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                listOfNotNull(tx.sender?.let { "From $it" }, tx.timestamp.formatDateTime()).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            tx.rawSms?.let { sms ->
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp),
                ) { Text(sms, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp) }
            }
            Spacer(Modifier.height(18.dp))
            SectionLabel("We think this is")
            Spacer(Modifier.height(6.dp))
            val signed = if (tx.type == TransactionType.CREDIT) tx.amountMinor else -tx.amountMinor
            Field("Amount", Money.format(signed, showPaise = true, signed = true))
            Field("Paid to", tx.merchant ?: "Unknown")
            Field("Category", tx.categoryEnum.label)
            Field("Account", tx.accountLabel ?: "Unknown")
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}
