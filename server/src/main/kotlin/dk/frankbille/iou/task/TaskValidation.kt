package dk.frankbille.iou.task

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
@Constraint(validatedBy = [TaskRecurrenceInputValidator::class])
annotation class ValidTaskRecurrence(
    val message: String = "Task recurrence is invalid",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

@Component
class TaskRecurrenceInputValidator : ConstraintValidator<ValidTaskRecurrence, TaskRecurrenceInput> {
    override fun isValid(
        input: TaskRecurrenceInput,
        context: ConstraintValidatorContext,
    ): Boolean {
        val errors = mutableListOf<Pair<String, String>>()

        if (input.interval != null && input.interval <= 0) {
            errors += "interval" to "Task recurrence interval must be greater than zero"
        }

        if (input.dayOfMonth != null && input.dayOfMonth !in 1..31) {
            errors += "dayOfMonth" to "Task recurrence dayOfMonth must be between 1 and 31"
        }

        if (input.maxCompletionsPerPeriod != null && input.maxCompletionsPerPeriod <= 0) {
            errors += "maxCompletionsPerPeriod" to "Task recurrence maxCompletionsPerPeriod must be greater than zero"
        }

        if (input.startsOn != null && input.endsOn != null && input.endsOn.isBefore(input.startsOn)) {
            errors += "endsOn" to "Task recurrence endsOn must not be before startsOn"
        }

        when (input.kind) {
            TaskRecurrenceKind.DAILY -> {
                if (!input.daysOfWeek.isNullOrEmpty()) {
                    errors += "daysOfWeek" to "Daily recurrence must not define daysOfWeek"
                }
                if (input.dayOfMonth != null) {
                    errors += "dayOfMonth" to "Daily recurrence must not define dayOfMonth"
                }
            }

            TaskRecurrenceKind.WEEKLY -> {
                if (input.daysOfWeek.isNullOrEmpty()) {
                    errors += "daysOfWeek" to "Weekly recurrence must define at least one dayOfWeek"
                }
                if (input.dayOfMonth != null) {
                    errors += "dayOfMonth" to "Weekly recurrence must not define dayOfMonth"
                }
            }

            TaskRecurrenceKind.MONTHLY -> {
                if (input.dayOfMonth == null) {
                    errors += "dayOfMonth" to "Monthly recurrence must define dayOfMonth"
                }
                if (!input.daysOfWeek.isNullOrEmpty()) {
                    errors += "daysOfWeek" to "Monthly recurrence must not define daysOfWeek"
                }
            }

            TaskRecurrenceKind.CUSTOM -> {
                if (input.interval == null) {
                    errors += "interval" to "Custom recurrence must define interval"
                }
                if (!input.daysOfWeek.isNullOrEmpty() && input.dayOfMonth != null) {
                    errors += "daysOfWeek" to "Custom recurrence must not define both daysOfWeek and dayOfMonth"
                }
            }
        }

        if (errors.isEmpty()) {
            return true
        }

        context.disableDefaultConstraintViolation()
        errors.forEach { (property, message) ->
            context
                .buildConstraintViolationWithTemplate(message)
                .addPropertyNode(property)
                .addConstraintViolation()
        }
        return false
    }
}
