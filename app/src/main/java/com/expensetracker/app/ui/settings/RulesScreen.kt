package com.expensetracker.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.AppContainer
import com.expensetracker.app.ui.components.MerchantAvatar
import com.expensetracker.core.model.Category
import kotlinx.coroutines.launch

/** Merchant → category rules created from "Always put X in Y". */
@Composable
fun RulesScreen(container: AppContainer, onBack: () -> Unit) {
    val rules by container.repository.observeRules().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(56.dp).padding(horizontal = 8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text("Categories & rules", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "When you choose “Always put … in …” while editing a transaction, it shows up here. " +
                "New payments to that merchant get the same category automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        if (rules.isEmpty()) {
            Text(
                "No rules yet.",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        LazyColumn {
            items(rules, key = { it.merchantKey }) { rule ->
                val category = Category.fromKey(rule.category)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    MerchantAvatar(rule.merchantName, category, size = 36.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(rule.merchantName, fontWeight = FontWeight.SemiBold)
                        Text(category.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { scope.launch { container.repository.deleteRule(rule) } }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Remove rule for ${rule.merchantName}")
                    }
                }
                HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}
