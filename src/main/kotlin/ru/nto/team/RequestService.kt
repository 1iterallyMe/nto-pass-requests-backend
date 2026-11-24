package ru.nto.team

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class RequestService(
    private val staffService: StaffService,
    private val requestRepository: PassRequestRepository,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun list(code: String, status: String?): List<RequestListDto> {
        val staff = staffService.requireStaff(code)
        val hostId = requireNotNull(staff.id)
        val filter = status?.let { parseStatus(it) }

        val requests = if (filter == null) {
            requestRepository.findByHostIdOrderByValidFromAscIdAsc(hostId)
        } else {
            requestRepository.findByHostIdAndStatusOrderByValidFromAscIdAsc(hostId, filter)
        }
        return requests.map { it.toListDto() }
    }

    @Transactional(readOnly = true)
    fun details(code: String, id: Long): RequestDetailsDto {
        val staff = staffService.requireStaff(code)
        val request = requestRepository.findOwned(id, requireNotNull(staff.id))
            ?: throw ApiException(404, "Request not found")
        return request.toDetailsDto()
    }

    @Transactional
    fun create(code: String, body: CreateRequestBody): RequestDetailsDto {
        val staff = staffService.requireStaff(code)
        val data = RequestValidation.validateCreate(body, clock)
        val hostId = requireNotNull(staff.id)

        val duplicates = requestRepository.countOverlapping(
            hostId = hostId,
            phone = data.phone,
            statuses = ACTIVE_STATUSES,
            newFrom = data.validFrom,
            newTo = data.validTo,
        )
        if (duplicates > 0) throw ApiException(409, "Duplicate request")

        val now = nowSeconds()
        val request = PassRequest(
            host = staff,
            fullName = data.fullName,
            phone = data.phone,
            organization = data.organization,
            purpose = data.purpose,
            validFrom = data.validFrom,
            validTo = data.validTo,
            status = RequestStatus.PENDING,
            comment = null,
            createdAt = now,
            updatedAt = now,
        )
        return requestRepository.save(request).toDetailsDto()
    }

    @Transactional
    fun cancel(code: String, id: Long): RequestDetailsDto {
        val staff = staffService.requireStaff(code)
        val request = requestRepository.findOwned(id, requireNotNull(staff.id))
            ?: throw ApiException(404, "Request not found")
        if (request.status != RequestStatus.PENDING) {
            throw ApiException(409, "Invalid status transition")
        }

        request.status = RequestStatus.CANCELLED
        request.updatedAt = nowSeconds()
        return requestRepository.save(request).toDetailsDto()
    }

    @Transactional
    fun decide(id: Long, body: DecisionBody): RequestDetailsDto {
        val request = requestRepository.findById(id).orElse(null)
            ?: throw ApiException(404, "Request not found")

        val decision = RequestValidation.validateDecision(body)
        if (request.status != RequestStatus.PENDING) {
            throw ApiException(409, "Invalid status transition")
        }

        request.status = decision.status
        request.comment = decision.comment
        request.updatedAt = nowSeconds()
        return requestRepository.save(request).toDetailsDto()
    }

    private fun parseStatus(raw: String): RequestStatus =
        RequestStatus.entries.firstOrNull { it.name == raw }
            ?: throw ApiException(400, "Invalid status")

    private fun nowSeconds(): LocalDateTime =
        LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS)

    private companion object {
        val ACTIVE_STATUSES = listOf(RequestStatus.PENDING, RequestStatus.APPROVED)
    }
}

