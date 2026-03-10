package dk.frankbille.iou.transaction

import org.springframework.data.jpa.repository.JpaRepository

interface TransactionRepository : JpaRepository<TransactionEntity, Long> {
    fun findAllByFamilyIdOrderByTimestampDesc(familyId: Long): List<TransactionEntity>
}
