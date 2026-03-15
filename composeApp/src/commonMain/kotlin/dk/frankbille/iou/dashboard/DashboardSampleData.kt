package dk.frankbille.iou.dashboard

internal fun sampleDashboardState(): DashboardState =
    DashboardState(
        familyName = "Birch House",
        houseNote = "A warm ledger for chores, rewards, and the little financial rules that make family life calmer.",
        currencySymbol = "$",
        children =
            listOf(
                ChildSnapshot(
                    name = "June",
                    balanceMinor = 6825,
                    savedMinor = 2400,
                    pendingTasks = 2,
                    subtitle = "Saving toward a sketch kit",
                    badgeLabel = "4 day streak",
                    accent = Pine,
                ),
                ChildSnapshot(
                    name = "Theo",
                    balanceMinor = 4150,
                    savedMinor = 900,
                    pendingTasks = 1,
                    subtitle = "Building up for soccer camp snacks",
                    badgeLabel = "6 day streak",
                    accent = Clay,
                ),
                ChildSnapshot(
                    name = "Iris",
                    balanceMinor = 2740,
                    savedMinor = 1300,
                    pendingTasks = 2,
                    subtitle = "Halfway to roller skates",
                    badgeLabel = "3 day streak",
                    accent = Gold,
                ),
            ),
        tasks =
            listOf(
                TaskSnapshot(
                    title = "Feed Rocket the rabbit",
                    childName = "June",
                    timingLabel = "ready after school",
                    rewardMinor = 125,
                    status = TaskStatus.REQUIRES_APPROVAL,
                    accent = Pine,
                ),
                TaskSnapshot(
                    title = "Laundry fold and sort",
                    childName = "Theo",
                    timingLabel = "before 7:00 PM",
                    rewardMinor = 225,
                    status = TaskStatus.SCHEDULED,
                    accent = Clay,
                ),
                TaskSnapshot(
                    title = "Water herbs and tomato bed",
                    childName = "Iris",
                    timingLabel = "auto pays tonight",
                    rewardMinor = 175,
                    status = TaskStatus.AUTO_PAY,
                    accent = Gold,
                ),
                TaskSnapshot(
                    title = "Saturday recycling run",
                    childName = "June",
                    timingLabel = "waiting on parent review",
                    rewardMinor = 350,
                    status = TaskStatus.REQUIRES_APPROVAL,
                    accent = Pine,
                ),
            ),
        accounts =
            listOf(
                AccountSnapshot(
                    name = "Pocket cash drawer",
                    amountMinor = 6350,
                    note = "Fast payouts for chores approved on the same day.",
                    fillRatio = 0.78f,
                    accent = Pine,
                ),
                AccountSnapshot(
                    name = "Family reserve",
                    amountMinor = 4215,
                    note = "The holding place for larger rewards and future transfers.",
                    fillRatio = 0.52f,
                    accent = Clay,
                ),
                AccountSnapshot(
                    name = "Savings jars",
                    amountMinor = 3150,
                    note = "Protected money that children can see but do not casually spend.",
                    fillRatio = 0.38f,
                    accent = Gold,
                ),
            ),
        activity =
            listOf(
                ActivitySnapshot(
                    title = "June's dog walk was approved",
                    detail = "Payout moved into Pocket cash drawer five minutes ago.",
                    amountLabel = "+$3.00",
                    accent = Pine,
                ),
                ActivitySnapshot(
                    title = "Theo shifted cash into savings",
                    detail = "A parent recorded a transfer from the reserve to Savings jars.",
                    amountLabel = "$2.50",
                    accent = Gold,
                ),
                ActivitySnapshot(
                    title = "Iris completed a quiet-time reading block",
                    detail = "This one is set to auto pay after dinner.",
                    amountLabel = "+$1.75",
                    accent = Clay,
                ),
            ),
        rules =
            listOf(
                RuleSnapshot(
                    title = "Balances come from history",
                    body = "Children should be able to understand where money came from and which action changed it.",
                    accent = Pine,
                ),
                RuleSnapshot(
                    title = "Approval is part of the product",
                    body = "Some chores need a parent check, so the UI should make that review rhythm visible instead of hiding it.",
                    accent = Clay,
                ),
                RuleSnapshot(
                    title = "Accounts are named places",
                    body = "Families think in jars, drawers, and held money. The design should preserve those mental models.",
                    accent = Gold,
                ),
            ),
    )
