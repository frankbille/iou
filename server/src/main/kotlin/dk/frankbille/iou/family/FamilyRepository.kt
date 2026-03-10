package dk.frankbille.iou.family

import org.springframework.data.jpa.repository.JpaRepository

interface FamilyRepository : JpaRepository<FamilyEntity, Long>
