package dk.frankbille.iou.events

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Service
class FamilyEventSubscriptionHub {
    private val sink = Sinks.many().multicast().directBestEffort<FamilyScopedEvent>()

    fun publishLocal(event: FamilyScopedEvent) {
        sink.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST)
    }

    fun events(familyId: Long): Flux<FamilyScopedEvent> = sink.asFlux().filter { it.familyId == familyId }
}
