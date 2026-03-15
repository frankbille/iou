package dk.frankbille.iou.parent

import dk.frankbille.iou.family.Person

data class Parent(
    override val id: Long,
    val name: String,
) : Person
