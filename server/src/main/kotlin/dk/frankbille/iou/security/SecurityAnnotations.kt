package dk.frankbille.iou.security

import org.springframework.security.access.prepost.PreAuthorize
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(CLASS, FUNCTION)
@Retention(RUNTIME)
@PreAuthorize(HAS_ACCESS_TO_FAMILY)
annotation class HasAccessToFamily

@Target(CLASS, FUNCTION)
@Retention(RUNTIME)
@PreAuthorize(IS_PARENT)
annotation class IsParent

@Target(CLASS, FUNCTION)
@Retention(RUNTIME)
@PreAuthorize(IS_CHILD)
annotation class IsChild

@Target(CLASS, FUNCTION)
@Retention(RUNTIME)
@PreAuthorize("($HAS_ACCESS_TO_FAMILY) and $IS_PARENT")
annotation class HasAccessToFamilyAndIsParent

private const val HAS_ACCESS_TO_FAMILY =
    "(#input != null and hasAuthority('FAMILY_' + #input.familyId)) or hasAuthority('FAMILY_' + #familyId)"

private const val IS_PARENT = "hasRole('PARENT')"
private const val IS_CHILD = "hasRole('CHILD')"
