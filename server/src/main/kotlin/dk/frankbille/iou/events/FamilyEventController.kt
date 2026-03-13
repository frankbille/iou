package dk.frankbille.iou.events

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux

@Controller
class FamilyEventController(
    private val familyEventService: FamilyEventService,
) {
    @SubscriptionMapping
    fun familyEvents(
        @Argument familyId: Long,
    ): Flux<FamilyScopedEvent> = familyEventService.familyEvents(familyId)
}
