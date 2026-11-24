package ru.nto.team

import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * Normalization and strict validation of incoming request bodies.
 *
 * All available field errors are collected and returned together as [FieldErrors].
 */
object RequestValidation {

    private val DATE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss").withResolverStyle(ResolverStyle.STRICT)

    private val PHONE_PATTERN = Regex("^\\+7[0-9]{10}$")

    data class CreateData(
        val fullName: String,
        val phone: String,
        val organization: String?,
        val purpose: String,
        val validFrom: LocalDateTime,
        val validTo: LocalDateTime,
    )

    data class DecisionData(val status: RequestStatus, val comment: String?)

    fun validateCreate(body: CreateRequestBody, clock: Clock): CreateData {
        val errors = LinkedHashMap<String, String>()

        val fullName = body.fullName?.trim()
        when {
            fullName.isNullOrEmpty() -> errors["fullName"] = "Full name is required"
            fullName.length !in 5..150 -> errors["fullName"] = "Full name must be between 5 and 150 characters"
        }

        val phone = body.phone?.trim()
        when {
            phone.isNullOrEmpty() -> errors["phone"] = "Phone is required"
            !PHONE_PATTERN.matches(phone) -> errors["phone"] = "Phone must match +7 and contain ten digits"
        }

        val organization = body.organization?.trim()?.takeIf { it.isNotEmpty() }
        if (organization != null && organization.length > 150) {
            errors["organization"] = "Organization must be at most 150 characters"
        }

        val purpose = body.purpose?.trim()
        when {
            purpose.isNullOrEmpty() -> errors["purpose"] = "Purpose is required"
            purpose.length !in 5..200 -> errors["purpose"] = "Purpose must be between 5 and 200 characters"
        }

        val validFrom = parseDate(body.validFrom)
        if (validFrom == null) {
            errors["validFrom"] = "validFrom must have format yyyy-MM-dd'T'HH:mm:ss"
        } else if (validFrom.atZone(clock.zone).toInstant().isBefore(clock.instant())) {
            errors["validFrom"] = "validFrom must not be in the past"
        }

        val validTo = parseDate(body.validTo)
        if (validTo == null) {
            errors["validTo"] = "validTo must have format yyyy-MM-dd'T'HH:mm:ss"
        } else if (validFrom != null) {
            when {
                !validTo.isAfter(validFrom) ->
                    errors["validTo"] = "validTo must be after validFrom"
                validTo.toLocalDate() != validFrom.toLocalDate() ->
                    errors["validTo"] = "validTo must be on the same calendar day as validFrom"
            }
        }

        if (errors.isNotEmpty()) throw FieldErrors(errors)

        return CreateData(
            fullName = fullName!!,
            phone = phone!!,
            organization = organization,
            purpose = purpose!!,
            validFrom = validFrom!!,
            validTo = validTo!!,
        )
    }

    fun validateDecision(body: DecisionBody): DecisionData {
        val errors = LinkedHashMap<String, String>()

        val requestedStatus = body.status?.trim()
            ?.let { raw -> RequestStatus.entries.firstOrNull { it.name == raw } }

        val target = when (requestedStatus) {
            RequestStatus.APPROVED, RequestStatus.REJECTED -> requestedStatus
            else -> {
                errors["status"] = "Status must be APPROVED or REJECTED"
                null
            }
        }

        val comment = body.comment?.trim()
        if (target == RequestStatus.REJECTED && comment.orEmpty().length !in 1..500) {
            errors["comment"] = "Comment is required and must be between 1 and 500 characters"
        }

        if (errors.isNotEmpty()) throw FieldErrors(errors)

        return DecisionData(
            status = requireNotNull(target),
            comment = if (target == RequestStatus.REJECTED) comment else null,
        )
    }

    private fun parseDate(raw: String?): LocalDateTime? {
        val value = raw?.trim()
        if (value.isNullOrEmpty()) return null
        return try {
            LocalDateTime.parse(value, DATE_FORMAT)
        } catch (_: Exception) {
            null
        }
    }
}

