package dk.frankbille.iou.e2e

import dk.frankbille.iou.child.ChildEntity
import dk.frankbille.iou.child.ChildRepository
import dk.frankbille.iou.family.CurrencyKind.ISO_CURRENCY
import dk.frankbille.iou.family.CurrencyPosition.PREFIX
import dk.frankbille.iou.family.FamilyChildEntity
import dk.frankbille.iou.family.FamilyChildRepository
import dk.frankbille.iou.family.FamilyEntity
import dk.frankbille.iou.family.FamilyParentEntity
import dk.frankbille.iou.family.FamilyParentRepository
import dk.frankbille.iou.family.FamilyRepository
import dk.frankbille.iou.invitation.ParentInvitationEntity
import dk.frankbille.iou.invitation.ParentInvitationRepository
import dk.frankbille.iou.invitation.ParentInvitationStatus
import dk.frankbille.iou.invitation.ParentInvitationStatus.ACCEPTED
import dk.frankbille.iou.moneyaccount.MoneyAccountEntity
import dk.frankbille.iou.moneyaccount.MoneyAccountKind
import dk.frankbille.iou.moneyaccount.MoneyAccountKind.BANK
import dk.frankbille.iou.moneyaccount.MoneyAccountKind.CASH
import dk.frankbille.iou.moneyaccount.MoneyAccountRepository
import dk.frankbille.iou.parent.ParentEntity
import dk.frankbille.iou.parent.ParentRepository
import dk.frankbille.iou.task.EligibilityMode
import dk.frankbille.iou.task.EligibilityMode.RESTRICTED
import dk.frankbille.iou.task.OneOffTaskEntity
import dk.frankbille.iou.task.OneOffTaskRepository
import dk.frankbille.iou.task.RecurringTaskCompletionEntity
import dk.frankbille.iou.task.RecurringTaskCompletionRepository
import dk.frankbille.iou.task.RecurringTaskEntity
import dk.frankbille.iou.task.RecurringTaskRepository
import dk.frankbille.iou.task.RecurringTaskStatus
import dk.frankbille.iou.task.RecurringTaskStatus.ARCHIVED
import dk.frankbille.iou.task.RewardPayoutPolicy
import dk.frankbille.iou.task.RewardPayoutPolicy.ON_APPROVAL
import dk.frankbille.iou.task.TaskCompletionStatus
import dk.frankbille.iou.task.TaskCompletionStatus.APPROVED
import dk.frankbille.iou.task.TaskCompletionStatus.COMPLETED
import dk.frankbille.iou.task.TaskRecurrenceKind
import dk.frankbille.iou.task.TaskRecurrenceKind.WEEKLY
import dk.frankbille.iou.taskcategory.TaskCategoryEntity
import dk.frankbille.iou.taskcategory.TaskCategoryRepository
import dk.frankbille.iou.test.IntegrationTestConfiguration
import dk.frankbille.iou.transaction.AdjustmentReason
import dk.frankbille.iou.transaction.AdjustmentReason.MANUAL_ADD
import dk.frankbille.iou.transaction.AdjustmentTransactionEntity
import dk.frankbille.iou.transaction.DepositTransactionEntity
import dk.frankbille.iou.transaction.RewardTransactionEntity
import dk.frankbille.iou.transaction.RewardTransactionRepository
import dk.frankbille.iou.transaction.TransactionRepository
import dk.frankbille.iou.transaction.TransferTransactionEntity
import dk.frankbille.iou.transaction.WithdrawalTransactionEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.Instant
import java.time.LocalDate

@SpringBootTest(
    properties = [
        "spring.liquibase.contexts=schema",
        "decorator.datasource.p6spy.logging=custom",
        "decorator.datasource.p6spy.custom-appender-class=dk.frankbille.iou.e2e.SeedDataCreator",
    ],
)
@Import(IntegrationTestConfiguration::class)
class TestDataIntegration {
    @Autowired
    private lateinit var familyRepository: FamilyRepository

    @Autowired
    private lateinit var parentRepository: ParentRepository

    @Autowired
    private lateinit var familyParentRepository: FamilyParentRepository

    @Autowired
    private lateinit var childRepository: ChildRepository

    @Autowired
    private lateinit var familyChildRepository: FamilyChildRepository

    @Autowired
    private lateinit var moneyAccountRepository: MoneyAccountRepository

    @Autowired
    private lateinit var taskCategoryRepository: TaskCategoryRepository

    @Autowired
    private lateinit var parentInvitationRepository: ParentInvitationRepository

    @Autowired
    private lateinit var oneOffTaskRepository: OneOffTaskRepository

    @Autowired
    private lateinit var recurringTaskRepository: RecurringTaskRepository

    @Autowired
    private lateinit var recurringTaskCompletionRepository: RecurringTaskCompletionRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var rewardTransactionRepository: RewardTransactionRepository

    @Test
    fun `create complete family`() {
        val family = familyRepository.save(family())

        with(family) {
            val parents =
                listOf(
                    addParent("Jane Doe", "Mom"),
                    addParent("John Doe", "Dad"),
                )

            val children =
                listOf(
                    addChild("Lucy", "Daughter"),
                    addChild("Dave", "Son", ON_APPROVAL),
                )

            val moneyAccounts =
                listOf(
                    addMoneyAccount("Mom's bank account", BANK),
                    addMoneyAccount("Drawer", CASH),
                    addMoneyAccount("Piggy bank", CASH),
                )

            setDefaultRewardAccount(moneyAccounts.first())

            val taskCategories =
                listOf(
                    addTaskCategory("Household"),
                    addTaskCategory("Gardening"),
                )

            val invitations =
                listOf(
                    addParentInvitation(
                        invitedByParent = parents.first(),
                        email = "grandma@example.com",
                        status = ACCEPTED,
                        resolvedParent = parents.last(),
                    ),
                )

            val oneOffTasks =
                listOf(
                    addOneOffTask(
                        title = "Clean your room",
                        category = taskCategories.first(),
                        createdByParent = parents.first(),
                        updatedByParent = parents.first(),
                        rewardAmountMinor = 500,
                    ),
                    addOneOffTask(
                        title = "Mow the lawn",
                        category = taskCategories.last(),
                        createdByParent = parents.first(),
                        updatedByParent = parents.last(),
                        rewardAmountMinor = 1200,
                        rewardPayoutPolicy = ON_APPROVAL,
                        eligibilityMode = RESTRICTED,
                        eligibleChildren = setOf(children.last()),
                        status = APPROVED,
                        completedChild = children.last(),
                        completedAt = instant("2026-03-01T10:15:30Z"),
                        approvedByParent = parents.last(),
                        approvedAt = instant("2026-03-01T12:30:45Z"),
                    ),
                )

            val recurringTask =
                addRecurringTask(
                    title = "Water the plants",
                    category = taskCategories.last(),
                    createdByParent = parents.first(),
                    updatedByParent = parents.last(),
                    rewardAmountMinor = 250,
                    rewardPayoutPolicy = ON_APPROVAL,
                    eligibilityMode = RESTRICTED,
                    eligibleChildren = children.toSet(),
                    status = ARCHIVED,
                    recurrenceKind = WEEKLY,
                    recurrenceInterval = 2,
                    recurrenceDays = setOf(MONDAY, THURSDAY),
                    recurrenceStartsOn = date("2026-01-01"),
                    recurrenceEndsOn = date("2026-03-31"),
                    recurrenceMaxCompletionsPerPeriod = 2,
                )

            addRecurringTaskCompletion(
                recurringTask = recurringTask,
                child = children.last(),
                status = COMPLETED,
                occurrenceDate = date("2026-03-02"),
                completedAt = instant("2026-03-02T07:00:00Z"),
            )

            val recurringCompletion =
                addRecurringTaskCompletion(
                    recurringTask = recurringTask,
                    child = children.first(),
                    status = APPROVED,
                    occurrenceDate = date("2026-03-05"),
                    completedAt = instant("2026-03-05T07:00:00Z"),
                    approvedByParent = parents.first(),
                    approvedAt = instant("2026-03-05T08:00:00Z"),
                )

            addRewardTransaction(
                ownerParent = parents.last(),
                child = children.last(),
                account = moneyAccounts.last(),
                amountMinor = 1200,
                oneOffTask = oneOffTasks.last(),
            )
            addRewardTransaction(
                ownerParent = parents.first(),
                child = children.first(),
                account = moneyAccounts.last(),
                amountMinor = 250,
                recurringTaskCompletion = recurringCompletion,
            )
            addDepositTransaction(
                ownerParent = parents.first(),
                child = children.first(),
                account = moneyAccounts.first(),
                amountMinor = 5000,
            )
            addWithdrawalTransaction(
                ownerParent = parents.last(),
                child = children.first(),
                account = moneyAccounts.first(),
                amountMinor = 450,
            )
            addTransferTransaction(
                ownerParent = parents.first(),
                child = children.last(),
                fromAccount = moneyAccounts.first(),
                toAccount = moneyAccounts.last(),
                amountMinor = 300,
            )
            addAdjustmentTransaction(
                ownerParent = parents.last(),
                child = children.last(),
                account = moneyAccounts.last(),
                amountMinor = 125,
                adjustmentReason = MANUAL_ADD,
            )

            assertThat(parentInvitationRepository.findAllByFamilyIdOrderByCreatedAtDesc(id!!)).hasSize(1)
            assertThat(taskRepositoryForFamily()).hasSize(3)
            assertThat(recurringTask.findOccurrences()).hasSize(2)
            assertThat(transactionRepository.findAllByFamilyIdOrderByTimestampDesc(id!!)).hasSize(6)
            assertThat(rewardTransactionRepository.findByOneOffTaskId(oneOffTasks.last().id!!)).isNotNull
            assertThat(rewardTransactionRepository.findByRecurringTaskCompletionId(recurringCompletion.id!!)).isNotNull
        }
    }

    private fun FamilyEntity.addParent(
        name: String,
        relation: String,
    ) = parentRepository.save(parent(name)).also { parent ->
        familyParentRepository.save(familyParent(this, parent, relation))
    }

    private fun FamilyEntity.addChild(
        name: String,
        relation: String,
        rewardPayoutPolicyOverride: RewardPayoutPolicy? = null,
    ) = childRepository.save(child(name)).also { child ->
        familyChildRepository.save(familyChild(this, child, relation, rewardPayoutPolicyOverride))
    }

    private fun FamilyEntity.addMoneyAccount(
        name: String,
        kind: MoneyAccountKind,
    ) = moneyAccountRepository.save(moneyAccount(this, name, kind))

    private fun FamilyEntity.addTaskCategory(name: String) =
        taskCategoryRepository
            .save(taskCategory(this, name))

    private fun FamilyEntity.addParentInvitation(
        invitedByParent: ParentEntity,
        email: String,
        status: ParentInvitationStatus,
        resolvedParent: ParentEntity? = null,
    ) = parentInvitationRepository.saveAndFlush(parentInvitation(this, invitedByParent, email, status, resolvedParent))

    private fun FamilyEntity.addOneOffTask(
        title: String,
        category: TaskCategoryEntity,
        createdByParent: ParentEntity,
        updatedByParent: ParentEntity,
        rewardAmountMinor: Int,
        rewardPayoutPolicy: RewardPayoutPolicy = RewardPayoutPolicy.ON_COMPLETION,
        eligibilityMode: EligibilityMode = EligibilityMode.ALL_CHILDREN,
        eligibleChildren: Set<ChildEntity> = emptySet(),
        status: TaskCompletionStatus = TaskCompletionStatus.AVAILABLE,
        completedChild: ChildEntity? = null,
        completedAt: Instant? = null,
        approvedByParent: ParentEntity? = null,
        approvedAt: Instant? = null,
    ) = oneOffTaskRepository.saveAndFlush(
        oneOffTask(
            family = this,
            title = title,
            category = category,
            createdByParent = createdByParent,
            updatedByParent = updatedByParent,
            rewardAmountMinor = rewardAmountMinor,
            rewardPayoutPolicy = rewardPayoutPolicy,
            eligibilityMode = eligibilityMode,
            eligibleChildren = eligibleChildren,
            status = status,
            completedChild = completedChild,
            completedAt = completedAt,
            approvedByParent = approvedByParent,
            approvedAt = approvedAt,
        ),
    )

    private fun FamilyEntity.addRecurringTask(
        title: String,
        category: TaskCategoryEntity,
        createdByParent: ParentEntity,
        updatedByParent: ParentEntity,
        rewardAmountMinor: Int,
        rewardPayoutPolicy: RewardPayoutPolicy = RewardPayoutPolicy.ON_COMPLETION,
        eligibilityMode: EligibilityMode = EligibilityMode.ALL_CHILDREN,
        eligibleChildren: Set<ChildEntity> = emptySet(),
        status: RecurringTaskStatus = RecurringTaskStatus.ACTIVE,
        recurrenceKind: TaskRecurrenceKind = TaskRecurrenceKind.DAILY,
        recurrenceInterval: Int? = null,
        recurrenceDayOfMonth: Int? = null,
        recurrenceDays: Set<DayOfWeek>? = null,
        recurrenceStartsOn: LocalDate? = null,
        recurrenceEndsOn: LocalDate? = null,
        recurrenceMaxCompletionsPerPeriod: Int? = null,
    ) = recurringTaskRepository.saveAndFlush(
        recurringTask(
            family = this,
            title = title,
            category = category,
            createdByParent = createdByParent,
            updatedByParent = updatedByParent,
            rewardAmountMinor = rewardAmountMinor,
            rewardPayoutPolicy = rewardPayoutPolicy,
            eligibilityMode = eligibilityMode,
            eligibleChildren = eligibleChildren,
            status = status,
            recurrenceKind = recurrenceKind,
            recurrenceInterval = recurrenceInterval,
            recurrenceDayOfMonth = recurrenceDayOfMonth,
            recurrenceDays = recurrenceDays,
            recurrenceStartsOn = recurrenceStartsOn,
            recurrenceEndsOn = recurrenceEndsOn,
            recurrenceMaxCompletionsPerPeriod = recurrenceMaxCompletionsPerPeriod,
        ),
    )

    private fun addRecurringTaskCompletion(
        recurringTask: RecurringTaskEntity,
        child: ChildEntity,
        status: TaskCompletionStatus,
        occurrenceDate: LocalDate,
        completedAt: Instant? = null,
        approvedByParent: ParentEntity? = null,
        approvedAt: Instant? = null,
    ) = recurringTaskCompletionRepository.saveAndFlush(
        recurringTaskCompletion(
            recurringTask = recurringTask,
            child = child,
            status = status,
            occurrenceDate = occurrenceDate,
            completedAt = completedAt,
            approvedByParent = approvedByParent,
            approvedAt = approvedAt,
        ),
    )

    private fun FamilyEntity.addRewardTransaction(
        ownerParent: ParentEntity,
        child: ChildEntity,
        account: MoneyAccountEntity,
        amountMinor: Int,
        oneOffTask: OneOffTaskEntity? = null,
        recurringTaskCompletion: RecurringTaskCompletionEntity? = null,
    ) = rewardTransactionRepository.saveAndFlush(
        rewardTransaction(this, ownerParent, child, account, amountMinor, oneOffTask, recurringTaskCompletion),
    )

    private fun FamilyEntity.addDepositTransaction(
        ownerParent: ParentEntity,
        child: ChildEntity,
        account: MoneyAccountEntity,
        amountMinor: Int,
    ) = transactionRepository.saveAndFlush(depositTransaction(this, ownerParent, child, account, amountMinor))

    private fun FamilyEntity.addWithdrawalTransaction(
        ownerParent: ParentEntity,
        child: ChildEntity,
        account: MoneyAccountEntity,
        amountMinor: Int,
    ) = transactionRepository.saveAndFlush(withdrawalTransaction(this, ownerParent, child, account, amountMinor))

    private fun FamilyEntity.addTransferTransaction(
        ownerParent: ParentEntity,
        child: ChildEntity,
        fromAccount: MoneyAccountEntity,
        toAccount: MoneyAccountEntity,
        amountMinor: Int,
    ) = transactionRepository.saveAndFlush(
        transferTransaction(
            this,
            ownerParent,
            child,
            fromAccount,
            toAccount,
            amountMinor,
        ),
    )

    private fun FamilyEntity.addAdjustmentTransaction(
        ownerParent: ParentEntity,
        child: ChildEntity,
        account: MoneyAccountEntity,
        amountMinor: Int,
        adjustmentReason: AdjustmentReason,
    ) = transactionRepository.saveAndFlush(
        adjustmentTransaction(
            this,
            ownerParent,
            child,
            account,
            amountMinor,
            adjustmentReason,
        ),
    )

    private fun FamilyEntity.setDefaultRewardAccount(moneyAccount: MoneyAccountEntity) =
        apply {
            defaultRewardAccount = moneyAccount
        }.also { familyRepository.saveAndFlush(it) }

    private fun FamilyEntity.taskRepositoryForFamily() =
        oneOffTaskRepository.findAllByFamilyIdOrderByCreatedAtDesc(id!!) +
            recurringTaskRepository.findAllByFamilyIdOrderByCreatedAtDesc(id!!)

    private fun family() =
        FamilyEntity().apply {
            name = "Test Family"
            currencyKind = ISO_CURRENCY
            currencyCode = "USD"
            currencyName = "US Dollar"
            currencySymbol = "$"
            currencyPosition = PREFIX
            currencyMinorUnit = 2
            recurringTaskCompletionGracePeriodDays = 7
        }

    private fun parent(parentName: String) =
        ParentEntity().apply {
            name = parentName
        }

    private fun familyParent(
        family: FamilyEntity,
        parent: ParentEntity,
        relation: String,
    ) = FamilyParentEntity().apply {
        this.familyId = family.id!!
        this.parent = parent
        this.relation = relation
    }

    private fun child(childName: String) =
        ChildEntity().apply {
            name = childName
        }

    private fun familyChild(
        family: FamilyEntity,
        child: ChildEntity,
        relation: String,
        rewardPayoutPolicyOverride: RewardPayoutPolicy? = null,
    ) = FamilyChildEntity().apply {
        this.familyId = family.id!!
        this.child = child
        this.relation = relation
        this.rewardPayoutPolicyOverride = rewardPayoutPolicyOverride
    }

    private fun moneyAccount(
        family: FamilyEntity,
        name: String,
        kind: MoneyAccountKind,
    ) = MoneyAccountEntity().apply {
        this.familyId = family.id!!
        this.name = name
        this.kind = kind
    }

    private fun taskCategory(
        family: FamilyEntity,
        name: String,
    ) = TaskCategoryEntity().apply {
        this.familyId = family.id!!
        this.name = name
    }

    private fun parentInvitation(
        family: FamilyEntity,
        invitedByParent: ParentEntity,
        email: String,
        status: ParentInvitationStatus,
        resolvedParent: ParentEntity?,
    ) = ParentInvitationEntity().apply {
        this.familyId = family.id!!
        this.invitedByParent = invitedByParent
        this.email = email
        this.status = status
        this.createdAt = instant("2026-02-01T09:30:00Z")
        this.acceptedAt = instant("2026-02-01T10:00:00Z")
        this.expiresAt = instant("2026-02-08T09:30:00Z")
        this.resolvedParent = resolvedParent
        this.invitationNonce = "nonce-grandma-accepted"
    }

    private fun oneOffTask(
        family: FamilyEntity,
        title: String,
        category: TaskCategoryEntity,
        createdByParent: ParentEntity,
        updatedByParent: ParentEntity,
        rewardAmountMinor: Int,
        rewardPayoutPolicy: RewardPayoutPolicy,
        eligibilityMode: EligibilityMode,
        eligibleChildren: Set<ChildEntity>,
        status: TaskCompletionStatus,
        completedChild: ChildEntity?,
        completedAt: Instant?,
        approvedByParent: ParentEntity?,
        approvedAt: Instant?,
    ) = OneOffTaskEntity().apply {
        familyId = family.id!!
        this.title = title
        description = "One-off task: $title"
        this.category = category
        this.rewardAmountMinor = rewardAmountMinor
        this.rewardPayoutPolicy = rewardPayoutPolicy
        this.eligibilityMode = eligibilityMode
        this.eligibleChildren = eligibleChildren.toMutableSet().takeIf { it.isNotEmpty() }
        this.createdByParent = createdByParent
        createdAt = instant("2026-02-20T08:00:00Z")
        this.updatedByParent = updatedByParent
        updatedAt = instant("2026-02-21T08:00:00Z")
        this.status = status
        this.completedChild = completedChild
        this.completedAt = completedAt
        this.approvedByParent = approvedByParent
        this.approvedAt = approvedAt
    }

    private fun recurringTask(
        family: FamilyEntity,
        title: String,
        category: TaskCategoryEntity,
        createdByParent: ParentEntity,
        updatedByParent: ParentEntity,
        rewardAmountMinor: Int,
        rewardPayoutPolicy: RewardPayoutPolicy,
        eligibilityMode: EligibilityMode,
        eligibleChildren: Set<ChildEntity>,
        status: RecurringTaskStatus,
        recurrenceKind: TaskRecurrenceKind,
        recurrenceInterval: Int?,
        recurrenceDayOfMonth: Int?,
        recurrenceDays: Set<DayOfWeek>?,
        recurrenceStartsOn: LocalDate?,
        recurrenceEndsOn: LocalDate?,
        recurrenceMaxCompletionsPerPeriod: Int?,
    ) = RecurringTaskEntity().apply {
        familyId = family.id!!
        this.title = title
        description = "Recurring task: $title"
        this.category = category
        this.rewardAmountMinor = rewardAmountMinor
        this.rewardPayoutPolicy = rewardPayoutPolicy
        this.eligibilityMode = eligibilityMode
        this.eligibleChildren = eligibleChildren.toMutableSet().takeIf { it.isNotEmpty() }
        this.createdByParent = createdByParent
        createdAt = instant("2026-01-01T08:00:00Z")
        this.updatedByParent = updatedByParent
        updatedAt = instant("2026-02-01T08:00:00Z")
        this.status = status
        this.recurrenceKind = recurrenceKind
        this.recurrenceInterval = recurrenceInterval
        this.recurrenceDayOfMonth = recurrenceDayOfMonth
        this.recurrenceDays = recurrenceDays
        this.recurrenceStartsOn = recurrenceStartsOn
        this.recurrenceEndsOn = recurrenceEndsOn
        this.recurrenceMaxCompletionsPerPeriod = recurrenceMaxCompletionsPerPeriod
    }

    private fun recurringTaskCompletion(
        recurringTask: RecurringTaskEntity,
        child: ChildEntity,
        status: TaskCompletionStatus,
        occurrenceDate: LocalDate,
        completedAt: Instant?,
        approvedByParent: ParentEntity?,
        approvedAt: Instant?,
    ) = RecurringTaskCompletionEntity().apply {
        recurringTaskId = recurringTask.id!!
        this.child = child
        this.status = status
        this.occurrenceDate = occurrenceDate
        this.completedAt = completedAt ?: Instant.EPOCH
        this.approvedByParent = approvedByParent
        this.approvedAt = approvedAt
    }

    private fun rewardTransaction(
        family: FamilyEntity,
        ownerParent: ParentEntity,
        child: ChildEntity,
        account: MoneyAccountEntity,
        amountMinor: Int,
        oneOffTask: OneOffTaskEntity?,
        recurringTaskCompletion: RecurringTaskCompletionEntity?,
    ) = RewardTransactionEntity().apply {
        familyId = family.id!!
        timestamp = Instant.parse("2026-03-04T08:00:00Z")
        this.amountMinor = amountMinor
        description = "Reward payout"
        this.ownerParent = ownerParent
        this.child = child
        accountOne = account
        this.oneOffTask = oneOffTask
        this.recurringTaskCompletion = recurringTaskCompletion
    }

    private fun depositTransaction(
        family: FamilyEntity,
        ownerParent: ParentEntity,
        child: ChildEntity,
        account: MoneyAccountEntity,
        amountMinor: Int,
    ) = DepositTransactionEntity().apply {
        familyId = family.id!!
        timestamp = Instant.parse("2026-02-15T08:00:00Z")
        this.amountMinor = amountMinor
        description = "Deposit"
        this.ownerParent = ownerParent
        this.child = child
        accountOne = account
    }

    private fun withdrawalTransaction(
        family: FamilyEntity,
        ownerParent: ParentEntity,
        child: ChildEntity,
        account: MoneyAccountEntity,
        amountMinor: Int,
    ) = WithdrawalTransactionEntity().apply {
        familyId = family.id!!
        timestamp = Instant.parse("2026-02-16T08:00:00Z")
        this.amountMinor = amountMinor
        description = "Withdrawal"
        this.ownerParent = ownerParent
        this.child = child
        accountOne = account
    }

    private fun transferTransaction(
        family: FamilyEntity,
        ownerParent: ParentEntity,
        child: ChildEntity,
        fromAccount: MoneyAccountEntity,
        toAccount: MoneyAccountEntity,
        amountMinor: Int,
    ) = TransferTransactionEntity().apply {
        familyId = family.id!!
        timestamp = Instant.parse("2026-02-17T08:00:00Z")
        this.amountMinor = amountMinor
        description = "Transfer"
        this.ownerParent = ownerParent
        this.child = child
        accountOne = fromAccount
        accountTwo = toAccount
    }

    private fun adjustmentTransaction(
        family: FamilyEntity,
        ownerParent: ParentEntity,
        child: ChildEntity,
        account: MoneyAccountEntity,
        amountMinor: Int,
        adjustmentReason: AdjustmentReason,
    ) = AdjustmentTransactionEntity().apply {
        familyId = family.id!!
        timestamp = Instant.parse("2026-02-18T08:00:00Z")
        this.amountMinor = amountMinor
        description = "Adjustment"
        this.ownerParent = ownerParent
        this.child = child
        accountOne = account
        this.adjustmentReason = adjustmentReason
    }

    private fun RecurringTaskEntity.findOccurrences() =
        recurringTaskCompletionRepository.findAllByRecurringTaskIdOrderByOccurrenceDateDesc(id!!)

    private fun instant(dateTimeString: String): Instant = Instant.parse(dateTimeString)

    private fun date(dateString: String): LocalDate = LocalDate.parse(dateString)
}
