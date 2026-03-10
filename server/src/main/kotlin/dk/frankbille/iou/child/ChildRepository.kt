package dk.frankbille.iou.child

import org.springframework.data.jpa.repository.JpaRepository

interface ChildRepository : JpaRepository<ChildEntity, Long>
