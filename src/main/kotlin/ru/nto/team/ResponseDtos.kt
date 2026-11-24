package ru.nto.team

import java.time.LocalDateTime

data class ProfileDto(val name: String)

data class RequestListDto(
    val id: Long,
    val fullName: String,
    val validFrom: LocalDateTime,
    val validTo: LocalDateTime,
    val status: String,
)

data class RequestDetailsDto(
    val id: Long,
    val fullName: String,
    val phone: String,
    val organization: String?,
    val purpose: String,
    val validFrom: LocalDateTime,
    val validTo: LocalDateTime,
    val status: String,
    val comment: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

internal fun PassRequest.toListDto(): RequestListDto = RequestListDto(
    id = requireNotNull(id) { "Pass request must be persisted" },
    fullName = fullName,
    validFrom = validFrom,
    validTo = validTo,
    status = status.name,
)

internal fun PassRequest.toDetailsDto(): RequestDetailsDto = RequestDetailsDto(
    id = requireNotNull(id) { "Pass request must be persisted" },
    fullName = fullName,
    phone = phone,
    organization = organization,
    purpose = purpose,
    validFrom = validFrom,
    validTo = validTo,
    status = status.name,
    comment = comment,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
