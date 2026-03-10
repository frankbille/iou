package dk.frankbille.iou.parent

import org.springframework.data.jpa.repository.JpaRepository

interface ParentRepository : JpaRepository<ParentEntity, Long>
