package com.henriquesebastiao.prumo.core.designsystem.component

/**
 * The icons the server stores by name (accounts and categories; D33 of the server) are the web's,
 * from Lucide (`landmark`, `utensils`, `heart-pulse`): each one's closest Material Symbol here. A
 * name the app doesn't know draws the generic [PrumoIcons.Category], as the web does.
 */
object ServerIcons {
    private val byName = mapOf(
        "landmark" to PrumoIcons.AccountBalance,
        "bank" to PrumoIcons.AccountBalance,
        "vault" to PrumoIcons.Savings,
        "piggy-bank" to PrumoIcons.Savings,
        "wallet" to PrumoIcons.AccountBalanceWallet,
        "banknote" to PrumoIcons.Payments,
        "trending-up" to PrumoIcons.TrendingUp,
        "utensils" to PrumoIcons.Restaurant,
        "house" to PrumoIcons.Home,
        "home" to PrumoIcons.Home,
        "bus" to PrumoIcons.DirectionsBus,
        "car" to PrumoIcons.DirectionsCar,
        "motorcycle" to PrumoIcons.TwoWheeler,
        "fuel" to PrumoIcons.LocalGasStation,
        "gauge" to PrumoIcons.Speed,
        "wrench" to PrumoIcons.Build,
        "heart-pulse" to PrumoIcons.HealthAndSafety,
        "graduation-cap" to PrumoIcons.School,
        "ticket" to PrumoIcons.Celebration,
        "shopping-bag" to PrumoIcons.ShoppingBag,
        "repeat" to PrumoIcons.Subscriptions,
        "tag" to PrumoIcons.Sell,
        "briefcase" to PrumoIcons.Work,
        "code-xml" to PrumoIcons.Code,
        "code" to PrumoIcons.Code,
        "book" to PrumoIcons.MenuBook,
        "book-open" to PrumoIcons.AutoStories,
        "card" to PrumoIcons.CreditCard,
        "credit-card" to PrumoIcons.CreditCard,
        "receipt" to PrumoIcons.ReceiptLong,
        "gift" to PrumoIcons.Redeem,
        "plane" to PrumoIcons.Flight,
        "shirt" to PrumoIcons.Checkroom,
        "laptop" to PrumoIcons.Computer,
        "monitor" to PrumoIcons.Computer,
        "smartphone" to PrumoIcons.Smartphone,
        "droplet" to PrumoIcons.WaterDrop,
        "paw-print" to PrumoIcons.Pets,
    )

    /** The Material Symbol for the server's icon [name]. */
    fun of(name: String?): Int = name?.let(byName::get) ?: PrumoIcons.Category
}
