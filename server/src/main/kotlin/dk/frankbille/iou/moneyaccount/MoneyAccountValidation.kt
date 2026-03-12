package dk.frankbille.iou.moneyaccount

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
@Constraint(
    validatedBy = [
        CreateMoneyAccountUniqueNameValidator::class,
        UpdateMoneyAccountUniqueNameValidator::class,
    ],
)
annotation class UniqueMoneyAccountName(
    val message: String = "Money account name must be unique within family",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class CreateMoneyAccountUniqueNameValidator(
    private val moneyAccountRepository: MoneyAccountRepository,
) : ConstraintValidator<UniqueMoneyAccountName, CreateMoneyAccountInput> {
    override fun isValid(
        input: CreateMoneyAccountInput,
        context: ConstraintValidatorContext,
    ): Boolean = validateUniqueName(input.familyId, null, input.name, context)

    private fun validateUniqueName(
        familyId: Long,
        existingMoneyAccountId: Long?,
        name: String,
        context: ConstraintValidatorContext,
    ): Boolean {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return true
        }

        val exists =
            when (existingMoneyAccountId) {
                null -> moneyAccountRepository.existsByFamilyIdAndName(familyId, normalizedName)
                else -> moneyAccountRepository.existsByFamilyIdAndNameAndIdNot(familyId, normalizedName, existingMoneyAccountId)
            }

        if (!exists) {
            return true
        }

        context.disableDefaultConstraintViolation()
        context
            .buildConstraintViolationWithTemplate(
                "Money account '$normalizedName' already exists in family $familyId",
            ).addPropertyNode("name")
            .addConstraintViolation()
        return false
    }
}

@Component
class UpdateMoneyAccountUniqueNameValidator(
    private val moneyAccountRepository: MoneyAccountRepository,
) : ConstraintValidator<UniqueMoneyAccountName, UpdateMoneyAccountInput> {
    override fun isValid(
        input: UpdateMoneyAccountInput,
        context: ConstraintValidatorContext,
    ): Boolean {
        val familyId = moneyAccountRepository.findFamilyIdById(input.moneyAccountId) ?: return true
        val normalizedName = input.name.trim()
        if (normalizedName.isBlank()) {
            return true
        }

        if (!moneyAccountRepository.existsByFamilyIdAndNameAndIdNot(familyId, normalizedName, input.moneyAccountId)) {
            return true
        }

        context.disableDefaultConstraintViolation()
        context
            .buildConstraintViolationWithTemplate(
                "Money account '$normalizedName' already exists in family $familyId",
            ).addPropertyNode("name")
            .addConstraintViolation()
        return false
    }
}
