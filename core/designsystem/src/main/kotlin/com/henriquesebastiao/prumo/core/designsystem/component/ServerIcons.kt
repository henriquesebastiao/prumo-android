package com.henriquesebastiao.prumo.core.designsystem.component

/**
 * The icons the server stores by name (accounts and categories; D33 of the server) are the web's,
 * Lucide names (`landmark`, `utensils`, `heart-pulse`): each one's closest Material Symbol here. A
 * name the app doesn't know draws the generic [PrumoIcons.Category], as the web does. The web's
 * `ui/icons.ts` and `modules/finance/helpers.ts` list the same names (D23).
 */
object ServerIcons {
    private val byName = mapOf(
        // Accounts
        "landmark" to PrumoIcons.AccountBalance,
        "bank" to PrumoIcons.AccountBalance,
        "vault" to PrumoIcons.Savings,
        "wallet" to PrumoIcons.AccountBalanceWallet,
        // Home
        "house" to PrumoIcons.Home,
        "home" to PrumoIcons.Home,
        "key" to PrumoIcons.Key,
        "building" to PrumoIcons.Apartment,
        "zap" to PrumoIcons.Bolt,
        "droplet" to PrumoIcons.WaterDrop,
        "flame" to PrumoIcons.LocalFireDepartment,
        "wifi" to PrumoIcons.Wifi,
        "smartphone" to PrumoIcons.Smartphone,
        "tv" to PrumoIcons.Tv,
        "sofa" to PrumoIcons.Weekend,
        "hammer" to PrumoIcons.Handyman,
        "spray-can" to PrumoIcons.CleaningServices,
        "washing-machine" to PrumoIcons.LocalLaundryService,
        // Food
        "utensils" to PrumoIcons.Restaurant,
        "shopping-cart" to PrumoIcons.ShoppingCart,
        "coffee" to PrumoIcons.LocalCafe,
        "pizza" to PrumoIcons.LocalPizza,
        "croissant" to PrumoIcons.BakeryDining,
        "apple" to PrumoIcons.Nutrition,
        "beer" to PrumoIcons.SportsBar,
        "ice-cream-cone" to PrumoIcons.Icecream,
        "cake" to PrumoIcons.Cake,
        // Transport
        "car" to PrumoIcons.DirectionsCar,
        "motorcycle" to PrumoIcons.TwoWheeler,
        "fuel" to PrumoIcons.LocalGasStation,
        "gauge" to PrumoIcons.Speed,
        "bus" to PrumoIcons.DirectionsBus,
        "train-front" to PrumoIcons.Train,
        "car-taxi-front" to PrumoIcons.LocalTaxi,
        "bike" to PrumoIcons.PedalBike,
        "square-parking" to PrumoIcons.LocalParking,
        "wrench" to PrumoIcons.Build,
        "plane" to PrumoIcons.Flight,
        "map" to PrumoIcons.Map,
        // Health and care
        "heart-pulse" to PrumoIcons.HealthAndSafety,
        "pill" to PrumoIcons.Medication,
        "stethoscope" to PrumoIcons.Stethoscope,
        "toothbrush" to PrumoIcons.Dentistry,
        "dumbbell" to PrumoIcons.FitnessCenter,
        "brain" to PrumoIcons.Psychology,
        "scissors" to PrumoIcons.ContentCut,
        "sparkles" to PrumoIcons.Spa,
        "glasses" to PrumoIcons.Eyeglasses,
        // Leisure
        "ticket" to PrumoIcons.ConfirmationNumber,
        "film" to PrumoIcons.Movie,
        "music" to PrumoIcons.MusicNote,
        "gamepad-2" to PrumoIcons.SportsEsports,
        "book" to PrumoIcons.MenuBook,
        "party-popper" to PrumoIcons.Celebration,
        "trophy" to PrumoIcons.EmojiEvents,
        "tent" to PrumoIcons.Camping,
        "tree-palm" to PrumoIcons.BeachAccess,
        "camera" to PrumoIcons.PhotoCamera,
        "palette" to PrumoIcons.Palette,
        // Personal and family
        "shirt" to PrumoIcons.Checkroom,
        "gem" to PrumoIcons.Diamond,
        "baby" to PrumoIcons.ChildCare,
        "users" to PrumoIcons.Group,
        "paw-print" to PrumoIcons.Pets,
        "gift" to PrumoIcons.Redeem,
        "heart-handshake" to PrumoIcons.VolunteerActivism,
        "church" to PrumoIcons.Church,
        // Work and study
        "briefcase" to PrumoIcons.Work,
        "laptop" to PrumoIcons.Computer,
        "monitor" to PrumoIcons.Computer,
        "code-xml" to PrumoIcons.Code,
        "code" to PrumoIcons.Code,
        "graduation-cap" to PrumoIcons.School,
        "book-open" to PrumoIcons.AutoStories,
        // Money and bills
        "banknote" to PrumoIcons.Payments,
        "trending-up" to PrumoIcons.TrendingUp,
        "piggy-bank" to PrumoIcons.Savings,
        "coins" to PrumoIcons.Paid,
        "receipt" to PrumoIcons.ReceiptLong,
        "card" to PrumoIcons.CreditCard,
        "credit-card" to PrumoIcons.CreditCard,
        "repeat" to PrumoIcons.Subscriptions,
        "scale" to PrumoIcons.Balance,
        "percent" to PrumoIcons.Percent,
        "shield" to PrumoIcons.Shield,
        "file-text" to PrumoIcons.Description,
        "shopping-bag" to PrumoIcons.ShoppingBag,
        "store" to PrumoIcons.Storefront,
        "package" to PrumoIcons.Package,
        "tag" to PrumoIcons.Sell,
    )

    /** Every name the app draws, for tests and pickers. */
    val names: Set<String> get() = byName.keys

    /** The Material Symbol for the server's icon [name]. */
    fun of(name: String?): Int = name?.let(byName::get) ?: PrumoIcons.Category
}
