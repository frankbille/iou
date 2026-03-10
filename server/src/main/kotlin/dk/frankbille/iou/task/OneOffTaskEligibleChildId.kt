package dk.frankbille.iou.task

import java.io.Serializable
import java.util.Objects

class OneOffTaskEligibleChildId() : Serializable {
    var oneOffTask: Long? = null
    var child: Long? = null

    constructor(oneOffTask: Long?, child: Long?) : this() {
        this.oneOffTask = oneOffTask
        this.child = child
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OneOffTaskEligibleChildId) {
            return false
        }

        return oneOffTask == other.oneOffTask && child == other.child
    }

    override fun hashCode(): Int = Objects.hash(oneOffTask, child)
}
