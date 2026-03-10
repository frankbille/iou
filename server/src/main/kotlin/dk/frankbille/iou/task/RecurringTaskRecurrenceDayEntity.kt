package dk.frankbille.iou.task

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@IdClass(RecurringTaskRecurrenceDayId::class)
@Table(name = "recurring_task_recurrence_days")
class RecurringTaskRecurrenceDayEntity {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_task_id", nullable = false)
    lateinit var recurringTask: RecurringTaskEntity

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 32, nullable = false)
    var dayOfWeek: DayOfWeek = DayOfWeek.MONDAY
}
