package dk.frankbille.iou.transaction

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TransactionRepository : JpaRepository<TransactionEntity, Long> {
    fun findAllByFamilyIdOrderByTimestampDesc(familyId: Long): List<TransactionEntity>

    fun findAllByChildIdOrderByTimestampDesc(childId: Long): List<TransactionEntity>

    fun countByFamilyIdAndChildId(
        familyId: Long,
        childId: Long,
    ): Long

    fun countByFamilyIdAndOwnerParentId(
        familyId: Long,
        ownerParentId: Long,
    ): Long

    @Query(
        value = """
        SELECT COUNT(*)
        FROM transactions t
        WHERE t.account_one_id = :moneyAccountId
           OR t.account_two_id = :moneyAccountId
        """,
        nativeQuery = true,
    )
    fun countReferencesByMoneyAccountId(moneyAccountId: Long): Long
}
