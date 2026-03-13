package dk.frankbille.iou.events

import dk.frankbille.iou.security.HasAccessToFamily
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux

@Service
@Transactional(readOnly = true)
class FamilyEventService(
    private val familyEventSubscriptionHub: FamilyEventSubscriptionHub,
) {
    @HasAccessToFamily
    fun familyEvents(familyId: Long): Flux<FamilyScopedEvent> = familyEventSubscriptionHub.events(familyId)
}
