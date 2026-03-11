package dk.frankbille.iou.task

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.DayOfWeek

@Converter
class DayOfWeekConverter : AttributeConverter<Set<DayOfWeek>, String> {
    override fun convertToDatabaseColumn(attribute: Set<DayOfWeek>?): String =
        attribute
            ?.sortedBy { it.name }
            ?.joinToString(",") { it.name }
            .orEmpty()

    override fun convertToEntityAttribute(dbData: String?): Set<DayOfWeek> {
        if (dbData.isNullOrBlank()) return emptySet()

        return dbData
            .split(",")
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(DayOfWeek::valueOf)
            .toSet()
    }
}
