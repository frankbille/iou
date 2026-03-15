package dk.frankbille.iou.auth

import org.springframework.data.jpa.repository.JpaRepository

interface ParentAuthCredentialRepository : JpaRepository<ParentAuthCredentialEntity, Long> {
    fun findByEmail(email: String): ParentAuthCredentialEntity?
}
