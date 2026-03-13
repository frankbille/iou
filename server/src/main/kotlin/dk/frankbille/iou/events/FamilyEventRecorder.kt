package dk.frankbille.iou.events

interface FamilyEventRecorder {
    fun record(event: FamilyScopedEvent)
}
