package dk.frankbille.iou.moneyaccount

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MoneyAccountRepository : JpaRepository<MoneyAccountEntity, Long> {
    fun findAllByFamilyIdOrderByNameAsc(familyId: Long): List<MoneyAccountEntity>

    fun existsByFamilyIdAndName(familyId: Long, name: String): Boolean

    fun existsByFamilyIdAndNameAndIdNot(
        familyId: Long,
        name: String,
        id: Long,
    ): Boolean

    @Query("SELECT ma.familyId FROM MoneyAccountEntity ma WHERE ma.id = :id")
    fun findFamilyIdById(id: Long): Long?
}
