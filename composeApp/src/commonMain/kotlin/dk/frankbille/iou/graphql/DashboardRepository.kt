package dk.frankbille.iou.graphql

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import dk.frankbille.iou.dashboard.AccountSnapshot
import dk.frankbille.iou.dashboard.ActivitySnapshot
import dk.frankbille.iou.dashboard.ChildSnapshot
import dk.frankbille.iou.dashboard.Clay
import dk.frankbille.iou.dashboard.DashboardState
import dk.frankbille.iou.dashboard.Gold
import dk.frankbille.iou.dashboard.Pine
import dk.frankbille.iou.dashboard.RuleSnapshot
import dk.frankbille.iou.dashboard.TaskSnapshot
import dk.frankbille.iou.dashboard.TaskStatus
import dk.frankbille.iou.dashboard.formatCurrency
import dk.frankbille.iou.family.CurrencyPosition
import dk.frankbille.iou.graphql.generated.ViewerDashboardQuery
import okio.Buffer
import kotlin.math.abs

internal enum class DashboardDataSource {
    CACHE,
    NETWORK,
}

internal data class DashboardDataResult(
    val dashboardState: DashboardState,
    val viewerSummary: String,
    val source: DashboardDataSource,
)

internal class GraphqlSessionStore(
    initialServerUrl: String = "http://localhost:8080/graphql",
    initialJwt: String = "",
) {
    var serverUrl by mutableStateOf(initialServerUrl)
    var jwt by mutableStateOf(initialJwt)

    fun trimmedServerUrl(): String = serverUrl.trim()

    fun bearerTokenOrNull(): String? = jwt.trim().takeIf { it.isNotEmpty() }

    fun tokenHash(): Int = bearerTokenOrNull()?.hashCode() ?: 0
}

internal class DashboardRepository(
    private val sessionStore: GraphqlSessionStore,
) {
    private val clientFactory = ApolloClientFactory(sessionStore = sessionStore)
    private val snapshotStorage = ViewerSnapshotStorage()

    suspend fun loadDashboard(): DashboardDataResult {
        readCachedDashboardOrNull()?.let {
            return it
        }

        return refreshDashboard()
    }

    suspend fun readCachedDashboard(): DashboardDataResult =
        readCachedDashboardOrNull()
            ?: throw IllegalStateException("No cached GraphQL dashboard snapshot is available yet.")

    suspend fun refreshDashboard(): DashboardDataResult {
        val client = clientFactory.client()
        hydratePersistentCache(client)
        val response =
            client
                .query(ViewerDashboardQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()

        val result = response.requireDashboard(source = DashboardDataSource.NETWORK)
        response.data?.let { data ->
            snapshotStorage.save(
                PersistedViewerSnapshot(
                    serverUrl = sessionStore.trimmedServerUrl(),
                    tokenHash = sessionStore.tokenHash(),
                    payload = serializeViewerDashboardData(data),
                ),
            )
        }

        return result
    }

    private suspend fun readCachedDashboardOrNull(): DashboardDataResult? {
        val client = clientFactory.client()
        hydratePersistentCache(client)
        val response =
            client
                .query(ViewerDashboardQuery())
                .fetchPolicy(FetchPolicy.CacheOnly)
                .execute()

        response.exception?.let {
            return null
        }

        return response.data?.viewer?.toDashboardResult(source = DashboardDataSource.CACHE)
    }

    private fun hydratePersistentCache(client: ApolloClient) {
        val snapshot =
            snapshotStorage
                .load()
                ?.takeIf {
                    it.serverUrl == sessionStore.trimmedServerUrl() &&
                        it.tokenHash == sessionStore.tokenHash()
                } ?: return

        runCatching {
            client.apolloStore.writeOperationSync(
                operation = ViewerDashboardQuery(),
                operationData = deserializeViewerDashboardData(snapshot.payload),
                customScalarAdapters = CustomScalarAdapters.Empty,
            )
        }.onFailure {
            snapshotStorage.clear()
        }
    }
}

private class ApolloClientFactory(
    private val sessionStore: GraphqlSessionStore,
) {
    private var currentSessionSignature: String? = null
    private var currentClient: ApolloClient? = null

    fun client(): ApolloClient {
        val serverUrl = sessionStore.trimmedServerUrl()
        require(serverUrl.isNotEmpty()) { "GraphQL URL is required." }
        val sessionSignature = "$serverUrl#${sessionStore.tokenHash()}"

        val existing = currentClient
        if (existing != null && currentSessionSignature == sessionSignature) {
            return existing
        }

        val apolloClient =
            ApolloClient
                .Builder()
                .serverUrl(serverUrl)
                .addHttpInterceptor(BearerTokenInterceptor(sessionStore = sessionStore))
                .normalizedCache(MemoryCacheFactory(maxSizeBytes = 5 * 1024 * 1024))
                .build()

        currentSessionSignature = sessionSignature
        currentClient = apolloClient
        return apolloClient
    }
}

private class BearerTokenInterceptor(
    private val sessionStore: GraphqlSessionStore,
) : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain,
    ): HttpResponse {
        val token =
            requireNotNull(sessionStore.bearerTokenOrNull()) {
                "JWT bearer token is required."
            }

        val authenticatedRequest =
            request
                .newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()

        return chain.proceed(authenticatedRequest)
    }
}

private fun com.apollographql.apollo.api.ApolloResponse<ViewerDashboardQuery.Data>.requireDashboard(
    source: DashboardDataSource,
): DashboardDataResult {
    exception?.let { throw IllegalStateException(it.message ?: "The GraphQL request failed.") }
    val graphqlError = errors?.firstOrNull()?.message
    if (graphqlError != null) {
        throw IllegalStateException(graphqlError)
    }

    return data
        ?.viewer
        ?.toDashboardResult(source)
        ?: throw IllegalStateException("The authenticated viewer does not have any accessible families.")
}

private fun ViewerDashboardQuery.Viewer.toDashboardResult(source: DashboardDataSource): DashboardDataResult {
    val family =
        families.firstOrNull()
            ?: throw IllegalStateException("The authenticated viewer does not have any accessible families.")

    val accentsByChildId = family.children.mapIndexed { index, familyChild -> familyChild.child.id to accentForIndex(index) }.toMap()
    val currencyPosition =
        when (family.currency.position.name) {
            "SUFFIX" -> CurrencyPosition.SUFFIX
            else -> CurrencyPosition.PREFIX
        }
    val moneyFormat =
        MoneyFormat(
            symbol = family.currency.symbol,
            position = currencyPosition,
            minorUnit = family.currency.minorUnit,
        )
    val tasks = family.tasks.mapNotNull { it.toTaskSnapshot(accentsByChildId) }.take(6)
    val children =
        family.children.mapIndexed { index, familyChild ->
            val child = familyChild.child
            val accent = accentForIndex(index)
            val pendingTasks = family.tasks.count { it.isRelevantToChild(child.id) }
            val savedMinor = child.transactions.sumOf { it.savingsDeltaMinor() }.coerceAtLeast(0)
            ChildSnapshot(
                name = child.name,
                balanceMinor = child.balance.amountMinor,
                savedMinor = savedMinor.coerceAtMost(child.balance.amountMinor.coerceAtLeast(0)),
                pendingTasks = pendingTasks,
                subtitle = familyChild.payoutLabel(),
                badgeLabel = familyChild.relation.ifBlank { "Family child" },
                accent = accent,
            )
        }
    val maxAccountBalance = family.moneyAccounts.maxOfOrNull { it.balance.amountMinor.coerceAtLeast(0) }?.coerceAtLeast(1) ?: 1
    val accounts =
        family.moneyAccounts.map { account ->
            val accent = accentForAccountKind(account.kind.name)
            AccountSnapshot(
                name = account.name,
                amountMinor = account.balance.amountMinor,
                note =
                    buildAccountNote(
                        kind = account.kind.name,
                        isDefaultRewardAccount = account.id == family.defaultRewardAccount.id,
                    ),
                fillRatio =
                    account.balance.amountMinor
                        .coerceAtLeast(0)
                        .toFloat() / maxAccountBalance.toFloat(),
                accent = accent,
            )
        }
    val activity =
        family.children
            .flatMap { familyChild ->
                familyChild.child.transactions.mapNotNull { transaction ->
                    transaction.toActivitySnapshot(
                        childName = familyChild.child.name,
                        moneyFormat = moneyFormat,
                        accent = accentsByChildId[familyChild.child.id] ?: Pine,
                    )
                }
            }.sortedByDescending { it.sortKey }
            .take(6)
            .map { it.activity }
    val pendingInvitations = family.parentInvitations.count { it.status.name == "PENDING" }
    val approvalsWaiting = tasks.count { it.status == TaskStatus.REQUIRES_APPROVAL }
    val viewerSummary =
        buildViewerSummary(
            viewer = person.viewerDisplayName(),
            familyCount = families.size,
        )

    return DashboardDataResult(
        dashboardState =
            DashboardState(
                familyName = family.name,
                houseNote =
                    buildHouseNote(
                        viewerSummary = viewerSummary,
                        parentCount = family.parents.size,
                        childCount = family.children.size,
                        pendingInvitations = pendingInvitations,
                        defaultRewardAccount = family.defaultRewardAccount.name,
                    ),
                currencySymbol = moneyFormat.symbol,
                currencyPosition = moneyFormat.position,
                currencyMinorUnit = moneyFormat.minorUnit,
                children = children,
                tasks = tasks,
                accounts = accounts,
                activity = activity,
                rules =
                    listOf(
                        RuleSnapshot(
                            title = "Rewards settle into ${family.defaultRewardAccount.name}",
                            body =
                                "Completed chores pay into the configured default reward account, giving the family one clear landing place for earned money.",
                            accent = accentForAccountKind(family.defaultRewardAccount.kind.name),
                        ),
                        RuleSnapshot(
                            title = "${family.recurringTaskCompletionGracePeriodDays} day recurring grace",
                            body =
                                if (family.recurringTaskCompletionGracePeriodDays == 0) {
                                    "Recurring chores can only be logged inside the current eligible recurrence period."
                                } else {
                                    "Recurring chores can still be logged ${family.recurringTaskCompletionGracePeriodDays} day(s) after the eligible period has passed."
                                },
                            accent = Clay,
                        ),
                        RuleSnapshot(
                            title = "$approvalsWaiting chores need a parent check",
                            body =
                                if (pendingInvitations == 0) {
                                    "${family.parents.size} parents are currently connected to this family, and approval-based rewards stay visible until one of them reviews the work."
                                } else {
                                    "${family.parents.size} parents are connected today, with $pendingInvitations invitation(s) still pending."
                                },
                            accent = Gold,
                        ),
                    ),
            ),
        viewerSummary = viewerSummary,
        source = source,
    )
}

private fun ViewerDashboardQuery.Person.viewerDisplayName(): String = onParent?.name ?: onChild?.name ?: "This viewer"

private fun ViewerDashboardQuery.Child.payoutLabel(): String =
    when (rewardPayoutPolicyOverride?.name) {
        "ON_APPROVAL" -> "Rewards for this child wait for approval."
        "ON_COMPLETION" -> "Rewards for this child pay immediately on completion."
        else -> "Rewards follow each task's payout policy."
    }

private fun ViewerDashboardQuery.Task.isRelevantToChild(childId: String): Boolean {
    val oneOffTask = onOneOffTask
    if (oneOffTask != null) {
        if (oneOffTask.status.name == "APPROVED") {
            return false
        }
        val eligibleChildren = eligibleChildren.orEmpty()
        return oneOffTask.child?.id == childId || eligibleChildren.isEmpty() || eligibleChildren.any { it.id == childId }
    }

    val recurringTask = onRecurringTask ?: return false
    if (recurringTask.recurringTaskStatus.name != "ACTIVE") {
        return false
    }
    val eligibleChildren = eligibleChildren.orEmpty()
    return eligibleChildren.isEmpty() || eligibleChildren.any { it.id == childId }
}

private fun ViewerDashboardQuery.Task.toTaskSnapshot(accentsByChildId: Map<String, Color>): TaskSnapshot? {
    val oneOffTask = onOneOffTask
    if (oneOffTask != null) {
        val eligibleChildren = eligibleChildren.orEmpty()
        val resolvedChildName =
            oneOffTask.child?.name ?: eligibleChildren.singleOrNull()?.name ?: eligibleChildrenLabel(eligibleChildren)
        val accent = accentForTask(oneOffTask.child?.id, eligibleChildren.firstOrNull()?.id, accentsByChildId)
        val statusName = oneOffTask.status.name
        return TaskSnapshot(
            title = title,
            childName = resolvedChildName,
            timingLabel = oneOffTaskTimingLabel(statusName = statusName, rewardPayoutPolicy = rewardPayoutPolicy.name),
            rewardMinor = reward.amountMinor,
            status = taskStatusFor(statusName = statusName, rewardPayoutPolicy = rewardPayoutPolicy.name),
            accent = accent,
        )
    }

    val recurringTask = onRecurringTask ?: return null
    val eligibleChildren = eligibleChildren.orEmpty()
    val latestCompletion = recurringTask.completions.maxByOrNull { it.occurrenceDate.toString() }
    val resolvedChildName =
        latestCompletion?.child?.name ?: eligibleChildren.singleOrNull()?.name ?: eligibleChildrenLabel(eligibleChildren)
    val accent = accentForTask(latestCompletion?.child?.id, eligibleChildren.firstOrNull()?.id, accentsByChildId)
    val statusName = latestCompletion?.status?.name ?: "AVAILABLE"
    return TaskSnapshot(
        title = title,
        childName = resolvedChildName,
        timingLabel = recurringTaskTimingLabel(recurringTask),
        rewardMinor = reward.amountMinor,
        status = taskStatusFor(statusName = statusName, rewardPayoutPolicy = rewardPayoutPolicy.name),
        accent = accent,
    )
}

private fun ViewerDashboardQuery.Transaction.savingsDeltaMinor(): Int {
    onRewardTransaction?.let { rewardTransaction ->
        return if (rewardTransaction.toAccount.kind.name == "SAVINGS") {
            amount.amountMinor
        } else {
            0
        }
    }
    onTransferTransaction?.let { transferTransaction ->
        return when {
            transferTransaction.toAccount.kind.name == "SAVINGS" -> amount.amountMinor
            transferTransaction.fromAccount.kind.name == "SAVINGS" -> -amount.amountMinor
            else -> 0
        }
    }
    onAdjustmentTransaction?.let { adjustmentTransaction ->
        if (adjustmentTransaction.account.kind.name != "SAVINGS") {
            return 0
        }
        return when (adjustmentTransaction.reason.name) {
            "MANUAL_REMOVE" -> -amount.amountMinor
            else -> amount.amountMinor
        }
    }
    onWithdrawalTransaction?.let { withdrawalTransaction ->
        return if (withdrawalTransaction.fromAccount.kind.name == "SAVINGS") {
            -amount.amountMinor
        } else {
            0
        }
    }
    onDepositTransaction?.let { depositTransaction ->
        return if (depositTransaction.toAccount.kind.name == "SAVINGS") {
            amount.amountMinor
        } else {
            0
        }
    }
    return 0
}

private fun ViewerDashboardQuery.Transaction.toActivitySnapshot(
    childName: String,
    moneyFormat: MoneyFormat,
    accent: Color,
): ActivityCandidate? {
    onRewardTransaction?.let { rewardTransaction ->
        return ActivityCandidate(
            sortKey = timestamp.toString(),
            activity =
                ActivitySnapshot(
                    title = "$childName earned ${description ?: titleFromAccount(rewardTransaction.toAccount.name)}",
                    detail = "${rewardTransaction.owner.name} paid the reward into ${rewardTransaction.toAccount.name}.",
                    amountLabel = moneyFormat.plus(amount.amountMinor),
                    accent = accent,
                ),
        )
    }
    onTransferTransaction?.let { transferTransaction ->
        return ActivityCandidate(
            sortKey = timestamp.toString(),
            activity =
                ActivitySnapshot(
                    title = "$childName moved money between accounts",
                    detail =
                        "${transferTransaction.owner.name} moved funds from " +
                            "${transferTransaction.fromAccount.name} to ${transferTransaction.toAccount.name}.",
                    amountLabel = moneyFormat.neutral(amount.amountMinor),
                    accent = accent,
                ),
        )
    }
    onAdjustmentTransaction?.let { adjustmentTransaction ->
        val isRemoval = adjustmentTransaction.reason.name == "MANUAL_REMOVE"
        return ActivityCandidate(
            sortKey = timestamp.toString(),
            activity =
                ActivitySnapshot(
                    title = "$childName received a manual balance update",
                    detail =
                        description
                            ?: "${adjustmentTransaction.owner.name} recorded a ${adjustmentTransaction.reason.name.lowercase().replace(
                                '_',
                                ' ',
                            )} change in ${adjustmentTransaction.account.name}.",
                    amountLabel = if (isRemoval) moneyFormat.minus(amount.amountMinor) else moneyFormat.plus(amount.amountMinor),
                    accent = accent,
                ),
        )
    }
    onWithdrawalTransaction?.let { withdrawalTransaction ->
        return ActivityCandidate(
            sortKey = timestamp.toString(),
            activity =
                ActivitySnapshot(
                    title = "$childName spent from ${withdrawalTransaction.fromAccount.name}",
                    detail = description ?: "${withdrawalTransaction.owner.name} recorded money leaving the tracked system.",
                    amountLabel = moneyFormat.minus(amount.amountMinor),
                    accent = accent,
                ),
        )
    }
    onDepositTransaction?.let { depositTransaction ->
        return ActivityCandidate(
            sortKey = timestamp.toString(),
            activity =
                ActivitySnapshot(
                    title = "$childName received a deposit",
                    detail = description ?: "${depositTransaction.owner.name} added funds into ${depositTransaction.toAccount.name}.",
                    amountLabel = moneyFormat.plus(amount.amountMinor),
                    accent = accent,
                ),
        )
    }
    return null
}

private fun buildViewerSummary(
    viewer: String,
    familyCount: Int,
): String =
    if (familyCount == 1) {
        "$viewer can access 1 family."
    } else {
        "$viewer can access $familyCount families."
    }

private fun buildHouseNote(
    viewerSummary: String,
    parentCount: Int,
    childCount: Int,
    pendingInvitations: Int,
    defaultRewardAccount: String,
): String =
    buildString {
        append(viewerSummary)
        append(" Showing the first family with ")
        append(parentCount)
        append(" parent(s), ")
        append(childCount)
        append(" child(ren)")
        if (pendingInvitations > 0) {
            append(", and ")
            append(pendingInvitations)
            append(" pending invitation(s)")
        }
        append(". Rewards land in ")
        append(defaultRewardAccount)
        append(".")
    }

private fun buildAccountNote(
    kind: String,
    isDefaultRewardAccount: Boolean,
): String {
    val base =
        when (kind) {
            "CASH" -> "Fast-access cash for immediate household payouts."
            "BANK" -> "Money tracked in a bank-style holding place."
            "SAVINGS" -> "Protected money meant to stay parked for longer goals."
            else -> "A custom money location the household can reason about explicitly."
        }
    return if (isDefaultRewardAccount) {
        "$base This is the default reward account."
    } else {
        base
    }
}

private fun oneOffTaskTimingLabel(
    statusName: String,
    rewardPayoutPolicy: String,
): String =
    when (statusName) {
        "COMPLETED" -> {
            "waiting on parent review"
        }

        "APPROVED" -> {
            "reward already approved"
        }

        else -> {
            if (rewardPayoutPolicy == "ON_COMPLETION") {
                "auto pays on completion"
            } else {
                "ready for completion"
            }
        }
    }

private fun recurringTaskTimingLabel(recurringTask: ViewerDashboardQuery.OnRecurringTask): String {
    val recurrence = recurringTask.recurrence
    val base =
        when (recurrence.kind.name) {
            "DAILY" -> "repeats daily"
            "WEEKLY" -> "repeats weekly"
            "MONTHLY" -> "repeats monthly"
            "CUSTOM" -> "runs on a custom rhythm"
            else -> "repeats on a schedule"
        }
    val maxPerPeriod = recurrence.maxCompletionsPerPeriod?.let { " · up to $it per period" }.orEmpty()
    return if (recurringTask.completions.any { it.status.name == "COMPLETED" }) {
        "$base · waiting for approval"
    } else {
        base + maxPerPeriod
    }
}

private fun taskStatusFor(
    statusName: String,
    rewardPayoutPolicy: String,
): TaskStatus =
    when {
        statusName == "COMPLETED" && rewardPayoutPolicy == "ON_APPROVAL" -> TaskStatus.REQUIRES_APPROVAL
        statusName == "APPROVED" || rewardPayoutPolicy == "ON_COMPLETION" -> TaskStatus.AUTO_PAY
        else -> TaskStatus.SCHEDULED
    }

private fun eligibleChildrenLabel(eligibleChildren: List<ViewerDashboardQuery.EligibleChildren>): String =
    when {
        eligibleChildren.isEmpty() -> "Any child"
        eligibleChildren.size == 1 -> eligibleChildren.first().name
        else -> "${eligibleChildren.size} children"
    }

private fun accentForTask(
    preferredChildId: String?,
    fallbackChildId: String?,
    accentsByChildId: Map<String, Color>,
): Color = accentsByChildId[preferredChildId] ?: accentsByChildId[fallbackChildId] ?: Clay

private fun accentForIndex(index: Int): Color =
    when (index % 3) {
        0 -> Pine
        1 -> Clay
        else -> Gold
    }

private fun accentForAccountKind(kind: String): Color =
    when (kind) {
        "CASH" -> Pine
        "BANK" -> Clay
        "SAVINGS" -> Gold
        else -> Clay
    }

private fun titleFromAccount(accountName: String): String = accountName.lowercase()

private data class MoneyFormat(
    val symbol: String,
    val position: CurrencyPosition,
    val minorUnit: Int,
) {
    fun plus(amountMinor: Int): String = "+${absolute(amountMinor)}"

    fun minus(amountMinor: Int): String = "-${absolute(amountMinor)}"

    fun neutral(amountMinor: Int): String = absolute(amountMinor)

    private fun absolute(amountMinor: Int): String =
        formatCurrency(
            amountMinor = abs(amountMinor),
            symbol = symbol,
            position = position,
            minorUnit = minorUnit,
        )
}

private data class ActivityCandidate(
    val sortKey: String,
    val activity: ActivitySnapshot,
)

private fun serializeViewerDashboardData(data: ViewerDashboardQuery.Data): String {
    val buffer = Buffer()
    val writer = BufferedSinkJsonWriter(buffer)
    ViewerDashboardQuery().adapter().toJson(writer, CustomScalarAdapters.Empty, data)
    return buffer.readUtf8()
}

private fun deserializeViewerDashboardData(payload: String): ViewerDashboardQuery.Data {
    val buffer = Buffer().writeUtf8(payload)
    val reader = BufferedSourceJsonReader(buffer)
    return ViewerDashboardQuery().adapter().fromJson(reader, CustomScalarAdapters.Empty)
}
