package dk.frankbille.iou.family

import dk.frankbille.iou.moneyaccount.MoneyAccountRepository
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.springframework.stereotype.Component
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass

@Target(CLASS)
@Retention(RUNTIME)
@Constraint(validatedBy = [DefaultRewardAccountBelongsToFamilyValidator::class])
annotation class DefaultRewardAccountBelongsToFamily(
    val message: String = "Default reward account must belong to the selected family",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class DefaultRewardAccountBelongsToFamilyValidator(
    private val moneyAccountRepository: MoneyAccountRepository,
) : ConstraintValidator<DefaultRewardAccountBelongsToFamily, UpdateFamilyInput> {
    override fun isValid(
        input: UpdateFamilyInput,
        context: ConstraintValidatorContext,
    ): Boolean {
        val accountFamilyId = moneyAccountRepository.findFamilyIdById(input.defaultRewardAccountId) ?: return true
        if (accountFamilyId == input.familyId) {
            return true
        }

        context.disableDefaultConstraintViolation()
        context
            .buildConstraintViolationWithTemplate(
                "Money account ${input.defaultRewardAccountId} does not belong to family ${input.familyId}",
            ).addPropertyNode("defaultRewardAccountId")
            .addConstraintViolation()
        return false
    }
}
