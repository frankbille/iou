package dk.frankbille.iou.events

interface FamilyEventDistributor {
    fun distribute(events: List<FamilyScopedEvent>)
}
