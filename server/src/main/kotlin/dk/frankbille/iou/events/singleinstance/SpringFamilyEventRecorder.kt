package dk.frankbille.iou.events.singleinstance

import dk.frankbille.iou.events.FamilyEventRecorder
import dk.frankbille.iou.events.FamilyScopedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager

data class RecordedFamilyEvent(
    val event: FamilyScopedEvent,
)

@Component
class SpringFamilyEventRecorder(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : FamilyEventRecorder {
    override fun record(event: FamilyScopedEvent) {
        check(TransactionSynchronizationManager.isActualTransactionActive()) {
            "Family events must be recorded inside an active transaction"
        }

        applicationEventPublisher.publishEvent(
            RecordedFamilyEvent(event),
        )
    }
}
