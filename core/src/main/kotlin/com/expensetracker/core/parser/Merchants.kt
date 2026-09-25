package com.expensetracker.core.parser

import com.expensetracker.core.model.Category
import com.expensetracker.core.model.TransactionType

/**
 * Turns raw payee text ("zomato@hdfcbank", "INDIAN OIL", "UPI/P2M/.../SWIGGY") into a clean
 * display name and a default category. User rules (stored in the app DB) always win over this.
 */
object Merchants {

    private data class Known(val name: String, val category: Category, val keywords: List<String>)

    private val KNOWN = listOf(
        Known("Swiggy", Category.FOOD, listOf("swiggy")),
        Known("Zomato", Category.FOOD, listOf("zomato")),
        Known("Domino's", Category.FOOD, listOf("domino")),
        Known("McDonald's", Category.FOOD, listOf("mcdonald")),
        Known("KFC", Category.FOOD, listOf("kfc")),
        Known("Starbucks", Category.FOOD, listOf("starbucks")),
        Known("Blinkit", Category.GROCERIES, listOf("blinkit", "grofers")),
        Known("Zepto", Category.GROCERIES, listOf("zepto")),
        Known("BigBasket", Category.GROCERIES, listOf("bigbasket", "bbnow")),
        Known("DMart", Category.GROCERIES, listOf("dmart", "avenue supermarts")),
        Known("Instamart", Category.GROCERIES, listOf("instamart")),
        Known("Amazon", Category.SHOPPING, listOf("amazon", "amzn")),
        Known("Flipkart", Category.SHOPPING, listOf("flipkart")),
        Known("Myntra", Category.SHOPPING, listOf("myntra")),
        Known("Ajio", Category.SHOPPING, listOf("ajio")),
        Known("Meesho", Category.SHOPPING, listOf("meesho")),
        Known("Decathlon", Category.SHOPPING, listOf("decathlon")),
        Known("Nykaa", Category.SHOPPING, listOf("nykaa")),
        Known("Uber", Category.TRANSPORT, listOf("uber")),
        Known("Ola", Category.TRANSPORT, listOf("olacabs", "ola cabs", "ani technologies")),
        Known("Rapido", Category.TRANSPORT, listOf("rapido")),
        Known("Namma Metro", Category.TRANSPORT, listOf("metro")),
        Known("FASTag", Category.TRANSPORT, listOf("fastag")),
        Known("Indian Oil", Category.FUEL, listOf("indian oil", "iocl", "indianoil")),
        Known("HP Petrol", Category.FUEL, listOf("hpcl", "hp petrol", "hindustan petroleum")),
        Known("Bharat Petroleum", Category.FUEL, listOf("bpcl", "bharat petroleum")),
        Known("Shell", Category.FUEL, listOf("shell")),
        Known("Airtel", Category.BILLS, listOf("airtel")),
        Known("Jio", Category.BILLS, listOf("jio")),
        Known("Vi", Category.BILLS, listOf("vodafone", "vi prepaid")),
        Known("BESCOM", Category.BILLS, listOf("bescom")),
        Known("Tata Power", Category.BILLS, listOf("tata power", "tatapower")),
        Known("Electricity bill", Category.BILLS, listOf("electricity", "mseb", "bses", "tneb")),
        Known("Netflix", Category.ENTERTAINMENT, listOf("netflix")),
        Known("Spotify", Category.ENTERTAINMENT, listOf("spotify")),
        Known("Prime Video", Category.ENTERTAINMENT, listOf("primevideo", "prime video")),
        Known("Hotstar", Category.ENTERTAINMENT, listOf("hotstar", "jiocinema")),
        Known("BookMyShow", Category.ENTERTAINMENT, listOf("bookmyshow", "bigtree")),
        Known("PVR INOX", Category.ENTERTAINMENT, listOf("pvr", "inox")),
        Known("Apollo Pharmacy", Category.HEALTH, listOf("apollo")),
        Known("PharmEasy", Category.HEALTH, listOf("pharmeasy")),
        Known("1mg", Category.HEALTH, listOf("1mg", "tata 1mg")),
        Known("Practo", Category.HEALTH, listOf("practo")),
        Known("IRCTC", Category.TRAVEL, listOf("irctc")),
        Known("MakeMyTrip", Category.TRAVEL, listOf("makemytrip", "mmt")),
        Known("Goibibo", Category.TRAVEL, listOf("goibibo")),
        Known("IndiGo", Category.TRAVEL, listOf("indigo", "interglobe")),
        Known("Air India", Category.TRAVEL, listOf("air india")),
        Known("Airbnb", Category.TRAVEL, listOf("airbnb")),
        Known("Zerodha", Category.TRANSFERS, listOf("zerodha")),
        Known("Groww", Category.TRANSFERS, listOf("groww")),
    )

    private val PHONE_HANDLE = Regex("""^\+?\d{10,12}$""")

    /** Clean display name, or null when there is nothing usable. */
    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        findKnown(raw)?.let { return it.name }
        val base = if ('@' in raw) raw.substringBefore('@') else raw
        if (PHONE_HANDLE.matches(base)) return "UPI transfer"
        val words = base
            .replace(Regex("""[._\-]+"""), " ")
            .replace(Regex("""\d{4,}"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (words.length < 2) return null
        return words.split(' ').joinToString(" ") { w ->
            if (w.length <= 3 && w.all { it.isUpperCase() }) w
            else w.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    /** Stable key used for "always categorise this merchant as X" rules. */
    fun key(name: String): String = name.lowercase().filter { it.isLetterOrDigit() }

    fun defaultCategory(merchant: String?, rawCandidate: String?, type: TransactionType): Category {
        if (type == TransactionType.CREDIT) return Category.INCOME
        val probe = listOfNotNull(rawCandidate, merchant).joinToString(" ")
        findKnown(probe)?.let { return it.category }
        if (merchant == "UPI transfer") return Category.TRANSFERS
        return Category.OTHER
    }

    private fun findKnown(text: String): Known? {
        val t = text.lowercase()
        return KNOWN.firstOrNull { k -> k.keywords.any { kw -> containsWord(t, kw) } }
    }

    private fun containsWord(text: String, keyword: String): Boolean {
        // Short keywords must match a whole word ("ola" must not match "coca cola").
        if (keyword.length > 4) return keyword in text
        return Regex("""(^|[^a-z0-9])${Regex.escape(keyword)}([^a-z0-9]|$)""").containsMatchIn(text)
    }
}
