package dk.frankbille.iou.events.singleinstance

import dk.frankbille.iou.events.FamilyEventDistributor
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AfterCommitFamilyEventListener(
    private val familyEventDistributor: FamilyEventDistributor,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onRecordedFamilyEvent(recorded: RecordedFamilyEvent) {
        familyEventDistributor.distribute(listOf(recorded.event))
    }
}
