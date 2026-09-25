package com.expensetracker.app.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.expensetracker.app.R
import com.expensetracker.app.data.ThemeMode
import com.expensetracker.core.analytics.RangePreset
import com.expensetracker.core.model.Category
import com.expensetracker.core.model.Channel

// The core module is plain JVM, so its enums carry English labels only. The app shows these
// translated names instead; never display the core `label` properties directly.

@StringRes
fun Category.labelRes(): Int = when (this) {
    Category.FOOD -> R.string.cat_food
    Category.GROCERIES -> R.string.cat_groceries
    Category.SHOPPING -> R.string.cat_shopping
    Category.TRANSPORT -> R.string.cat_transport
    Category.FUEL -> R.string.cat_fuel
    Category.BILLS -> R.string.cat_bills
    Category.ENTERTAINMENT -> R.string.cat_entertainment
    Category.HEALTH -> R.string.cat_health
    Category.TRAVEL -> R.string.cat_travel
    Category.TRANSFERS -> R.string.cat_transfers
    Category.INCOME -> R.string.cat_income
    Category.OTHER -> R.string.cat_other
}

@StringRes
fun Channel.labelRes(): Int = when (this) {
    Channel.UPI -> R.string.channel_upi
    Channel.CARD -> R.string.channel_card
    Channel.NET_BANKING -> R.string.channel_net_banking
    Channel.ATM -> R.string.channel_atm
    Channel.WALLET -> R.string.channel_wallet
    Channel.AUTO_DEBIT -> R.string.channel_auto_debit
    Channel.OTHER -> R.string.channel_other
}

@StringRes
fun RangePreset.labelRes(): Int = when (this) {
    RangePreset.THIS_MONTH -> R.string.range_this_month
    RangePreset.LAST_3_MONTHS -> R.string.range_3m
    RangePreset.LAST_6_MONTHS -> R.string.range_6m
    RangePreset.LAST_YEAR -> R.string.range_1y
    RangePreset.LAST_3_YEARS -> R.string.range_3y
    RangePreset.ALL_TIME -> R.string.range_all
    RangePreset.CUSTOM -> R.string.range_custom
}

@StringRes
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable fun Category.displayName(): String = stringResource(labelRes())
@Composable fun Channel.displayName(): String = stringResource(labelRes())
@Composable fun RangePreset.displayName(): String = stringResource(labelRes())
@Composable fun ThemeMode.displayName(): String = stringResource(labelRes())

/** For code outside composition (PDF export, view models): pass a localized Activity context. */
fun Category.displayName(context: Context): String = context.getString(labelRes())
fun Channel.displayName(context: Context): String = context.getString(labelRes())
