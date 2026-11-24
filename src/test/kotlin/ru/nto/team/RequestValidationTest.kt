package ru.nto.team

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class RequestValidationTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun clockAt(dateTime: LocalDateTime): Clock =
        Clock.fixed(dateTime.atZone(zone).toInstant(), zone)

    private val clock = clockAt(LocalDateTime.of(2026, 10, 14, 10, 0, 0, 500_000_000))

    private fun body(
        fullName: String? = "Гость Иванович",
        phone: String? = "+79991234567",
        organization: String? = null,
        purpose: String? = "Рабочая встреча",
        validFrom: String? = "2026-10-14T10:00:01",
        validTo: String? = "2026-10-14T11:00:00",
    ) = CreateRequestBody(fullName, phone, organization, purpose, validFrom, validTo)

    private fun errors(block: () -> Unit): Map<String, String> =
        assertThrows(FieldErrors::class.java) { block() }.errors

    @Test
    fun `valid body is normalized and accepted`() {
        val data = RequestValidation.validateCreate(
            body(fullName = "  Гость Иванович  ", organization = "   ", purpose = "  Рабочая встреча "),
            clock,
        )

        assertEquals("Гость Иванович", data.fullName)
        assertEquals("+79991234567", data.phone)
        assertNull(data.organization)
        assertEquals("Рабочая встреча", data.purpose)
        assertEquals(LocalDateTime.of(2026, 10, 14, 10, 0, 1), data.validFrom)
        assertEquals(LocalDateTime.of(2026, 10, 14, 11, 0, 0), data.validTo)
    }

    @Test
    fun `validFrom before the exact clock instant is rejected`() {
        val errors = errors { RequestValidation.validateCreate(body(validFrom = "2026-10-14T10:00:00"), clock) }
        assertTrue(errors.containsKey("validFrom"))
    }

    @Test
    fun `validFrom equal to a whole-second clock is accepted`() {
        val wholeSecondClock = clockAt(LocalDateTime.of(2026, 10, 14, 10, 0, 0))
        val data = RequestValidation.validateCreate(body(validFrom = "2026-10-14T10:00:00"), wholeSecondClock)
        assertEquals(LocalDateTime.of(2026, 10, 14, 10, 0, 0), data.validFrom)
    }

    @Test
    fun `all field errors are collected together`() {
        val errors = errors {
            RequestValidation.validateCreate(
                body(
                    fullName = " ab ",
                    phone = "+7 999 123 45 67",
                    purpose = " abc ",
                    validFrom = "2026-10-14 10:00:01",
                    validTo = "not-a-date",
                ),
                clock,
            )
        }

        assertEquals(setOf("fullName", "phone", "purpose", "validFrom", "validTo"), errors.keys)
    }

    @Test
    fun `strict dates reject invalid calendar values and fractions`() {
        for (invalid in listOf(
            "2026-02-30T10:00:01",
            "2026-10-14 10:00:01",
            "2026-10-14T10:00:01.5",
            "2026-10-14T25:00:01",
        )) {
            val errors = errors { RequestValidation.validateCreate(body(validFrom = invalid), clock) }
            assertTrue(errors.containsKey("validFrom"), "expected validFrom error for $invalid")
        }
    }

    @Test
    fun `validTo must be after validFrom and on the same day`() {
        val notAfter = errors { RequestValidation.validateCreate(body(validTo = "2026-10-14T10:00:01"), clock) }
        assertTrue(notAfter.containsKey("validTo"))

        val nextDay = errors { RequestValidation.validateCreate(body(validTo = "2026-10-15T11:00:00"), clock) }
        assertTrue(nextDay.containsKey("validTo"))
    }

    @Test
    fun `approved decision ignores comment and trims status`() {
        val decision = RequestValidation.validateDecision(
            DecisionBody(status = "  APPROVED  ", comment = "x".repeat(1000)),
        )
        assertEquals(RequestStatus.APPROVED, decision.status)
        assertNull(decision.comment)
    }

    @Test
    fun `rejected decision requires a bounded comment`() {
        assertTrue(errors {
            RequestValidation.validateDecision(DecisionBody(status = "REJECTED", comment = "   "))
        }.containsKey("comment"))

        assertTrue(errors {
            RequestValidation.validateDecision(DecisionBody(status = "REJECTED", comment = "x".repeat(501)))
        }.containsKey("comment"))

        val decision = RequestValidation.validateDecision(
            DecisionBody(status = " REJECTED ", comment = "  Нет сотрудника  "),
        )
        assertEquals(RequestStatus.REJECTED, decision.status)
        assertEquals("Нет сотрудника", decision.comment)
    }

    @Test
    fun `unknown or lower-case target status is rejected`() {
        for (status in listOf(null, "", "approved", "CANCELLED")) {
            val errors = errors { RequestValidation.validateDecision(DecisionBody(status = status)) }
            assertTrue(errors.containsKey("status"), "expected status error for $status")
        }
    }

    @Test
    fun `outer spaces around target status are trimmed`() {
        val decision = RequestValidation.validateDecision(DecisionBody(status = "  APPROVED  "))
        assertEquals(RequestStatus.APPROVED, decision.status)
    }
}
