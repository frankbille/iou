package dk.frankbille.iou.events.singleinstance

import dk.frankbille.iou.events.FamilyEventDistributor
import dk.frankbille.iou.events.FamilyEventSubscriptionHub
import dk.frankbille.iou.events.FamilyScopedEvent
import org.springframework.stereotype.Component

@Component
class InMemoryFamilyEventDistributor(
    private val familyEventSubscriptionHub: FamilyEventSubscriptionHub,
) : FamilyEventDistributor {
    override fun distribute(events: List<FamilyScopedEvent>) {
        events.forEach(familyEventSubscriptionHub::publishLocal)
    }
}
