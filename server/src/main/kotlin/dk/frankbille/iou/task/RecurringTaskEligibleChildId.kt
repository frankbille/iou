package dk.frankbille.iou.task

import java.io.Serializable
import java.util.Objects

class RecurringTaskEligibleChildId() : Serializable {
    var recurringTask: Long? = null
    var child: Long? = null

    constructor(recurringTask: Long?, child: Long?) : this() {
        this.recurringTask = recurringTask
        this.child = child
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is RecurringTaskEligibleChildId) {
            return false
        }

        return recurringTask == other.recurringTask && child == other.child
    }

    override fun hashCode(): Int = Objects.hash(recurringTask, child)
}
