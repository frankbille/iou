package dk.frankbille.iou.security

import org.intellij.lang.annotations.Language
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(CLASS, FUNCTION)
@Retention(RUNTIME)
annotation class FamilyScopeCheck(
    @Language("SpEL")
    val familyIdExpression: String,
)
