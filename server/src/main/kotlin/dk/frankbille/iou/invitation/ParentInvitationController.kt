package dk.frankbille.iou.invitation

import dk.frankbille.iou.family.Family
import dk.frankbille.iou.family.FamilyService
import dk.frankbille.iou.parent.Parent
import dk.frankbille.iou.parent.ParentService
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class ParentInvitationController(
    private val familyService: FamilyService,
    private val parentService: ParentService,
) {
    @SchemaMapping(typeName = "ParentInvitation", field = "family")
    fun family(invitation: ParentInvitation): Family = familyService.getFamily(invitation.familyId)

    @SchemaMapping(typeName = "ParentInvitation", field = "invitedBy")
    fun invitedBy(invitation: ParentInvitation): Parent = invitation.invitedBy

    @SchemaMapping(typeName = "ParentInvitation", field = "parent")
    fun parent(invitation: ParentInvitation): Parent? = invitation.resolvedParentId?.let(parentService::getParent)
}
