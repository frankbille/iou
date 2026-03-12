package dk.frankbille.iou.security

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.aop.support.AopUtils
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Aspect
@Component
class FamilyScopeCheckAspect(
    private val familyScopeResolver: FamilyScopeResolver,
) {
    private val parser = SpelExpressionParser()
    private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

    @Around("@annotation(dk.frankbille.iou.security.FamilyScopeCheck) || @within(dk.frankbille.iou.security.FamilyScopeCheck)")
    fun enforceFamilyScope(joinPoint: ProceedingJoinPoint): Any? {
        val method = resolveMethod(joinPoint)
        val annotation =
            AnnotatedElementUtils.findMergedAnnotation(method, FamilyScopeCheck::class.java)
                ?: AnnotatedElementUtils.findMergedAnnotation(joinPoint.target.javaClass, FamilyScopeCheck::class.java)
                ?: return joinPoint.proceed()

        val authentication = SecurityContextHolder.getContext().authentication
        val familyId = resolveFamilyId(annotation.familyIdExpression, method, joinPoint, authentication)

        if (!hasAccessToFamily(authentication, familyId)) {
            throw NotFoundInFamilyException()
        }

        return joinPoint.proceed()
    }

    private fun resolveMethod(joinPoint: ProceedingJoinPoint) =
        AopUtils.getMostSpecificMethod((joinPoint.signature as MethodSignature).method, joinPoint.target.javaClass)

    private fun resolveFamilyId(
        expression: String,
        method: java.lang.reflect.Method,
        joinPoint: ProceedingJoinPoint,
        authentication: Authentication?,
    ): Long {
        val evaluationContext =
            MethodBasedEvaluationContext(
                FamilyScopeExpressionRoot(authentication),
                method,
                joinPoint.args,
                parameterNameDiscoverer,
            ).apply {
                setVariable("familyScopeResolver", familyScopeResolver)
            }

        val normalizedExpression = expression.replace("@familyScopeResolver", "#familyScopeResolver")
        val value = parser.parseExpression(normalizedExpression).getValue(evaluationContext)
        return when (value) {
            null -> throw NotFoundInFamilyException()
            is Long -> value
            is Int -> value.toLong()
            is Number -> value.toLong()
            else -> throw IllegalArgumentException("Family scope expression must resolve to a numeric family id")
        }
    }

    private fun hasAccessToFamily(
        authentication: Authentication?,
        familyId: Long,
    ): Boolean = authentication?.authorities?.any { it.authority == "FAMILY_$familyId" } == true
}
