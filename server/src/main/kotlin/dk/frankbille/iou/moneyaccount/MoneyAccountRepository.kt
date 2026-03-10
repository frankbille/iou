package dk.frankbille.iou.moneyaccount

import org.springframework.data.jpa.repository.JpaRepository

interface MoneyAccountRepository : JpaRepository<MoneyAccountEntity, Long> {
    fun findAllByFamilyIdOrderByNameAsc(familyId: Long): List<MoneyAccountEntity>

    fun existsByFamilyIdAndName(familyId: Long, name: String): Boolean
}
