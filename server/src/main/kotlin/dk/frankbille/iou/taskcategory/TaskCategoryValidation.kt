package dk.frankbille.iou.taskcategory

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
@Constraint(validatedBy = [UniqueTaskCategoryNameValidator::class])
annotation class UniqueTaskCategoryName(
    val message: String = "Task category name must be unique within family",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class UniqueTaskCategoryNameValidator(
    private val taskCategoryRepository: TaskCategoryRepository,
) : ConstraintValidator<UniqueTaskCategoryName, CreateTaskCategoryInput> {
    override fun isValid(
        createTaskCategoryInput: CreateTaskCategoryInput,
        context: ConstraintValidatorContext,
    ): Boolean {
        val normalizedName = createTaskCategoryInput.name.trim()
        if (normalizedName.isBlank()) {
            return true
        }

        if (!taskCategoryRepository.existsByFamilyIdAndName(createTaskCategoryInput.familyId, normalizedName)) {
            return true
        }

        context.disableDefaultConstraintViolation()
        context
            .buildConstraintViolationWithTemplate(
                "Task category '$normalizedName' already exists in family ${createTaskCategoryInput.familyId}",
            ).addPropertyNode("name")
            .addConstraintViolation()
        return false
    }
}
