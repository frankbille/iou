package dk.frankbille.iou.task

import java.io.Serializable
import java.util.Objects

class RecurringTaskRecurrenceDayId() : Serializable {
    var recurringTask: Long? = null
    var dayOfWeek: DayOfWeek? = null

    constructor(recurringTask: Long?, dayOfWeek: DayOfWeek?) : this() {
        this.recurringTask = recurringTask
        this.dayOfWeek = dayOfWeek
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is RecurringTaskRecurrenceDayId) {
            return false
        }

        return recurringTask == other.recurringTask && dayOfWeek == other.dayOfWeek
    }

    override fun hashCode(): Int = Objects.hash(recurringTask, dayOfWeek)
}
