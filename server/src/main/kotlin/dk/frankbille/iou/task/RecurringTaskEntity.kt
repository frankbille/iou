package dk.frankbille.iou.task

import dk.frankbille.iou.task.RecurringTaskStatus.ACTIVE
import dk.frankbille.iou.task.TaskRecurrenceKind.DAILY
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import java.time.DayOfWeek
import java.time.LocalDate

@Entity
@DiscriminatorValue("RECURRING")
@Table(name = "recurring_tasks")
@PrimaryKeyJoinColumn(name = "task_id")
class RecurringTaskEntity : TaskEntity() {
    @Enumerated(STRING)
    @Column(name = "status", length = 32, nullable = false)
    var status: RecurringTaskStatus = ACTIVE

    @Enumerated(STRING)
    @Column(name = "recurrence_kind", length = 32, nullable = false)
    var recurrenceKind: TaskRecurrenceKind = DAILY

    @Column(name = "recurrence_interval")
    var recurrenceInterval: Int? = null

    @Column(name = "recurrence_day_of_month")
    var recurrenceDayOfMonth: Int? = null

    @Convert(converter = DayOfWeekConverter::class)
    @Column(name = "recurrence_days")
    var recurrenceDays: Set<DayOfWeek>? = null

    @Column(name = "recurrence_starts_on")
    var recurrenceStartsOn: LocalDate? = null

    @Column(name = "recurrence_ends_on")
    var recurrenceEndsOn: LocalDate? = null

    @Column(name = "recurrence_max_completions_per_period")
    var recurrenceMaxCompletionsPerPeriod: Int? = null
}
