package ru.nto.team

import org.springframework.stereotype.Service

@Service
class StaffService(private val staffRepository: StaffRepository) {

    fun profile(code: String): ProfileDto {
        val staff = requireStaff(code)
        return ProfileDto(staff.fullName)
    }

    /**
     * Validates the employee code format and resolves the corresponding employee.
     * Throws 400 for a malformed code and 401 for an unknown one.
     */
    fun requireStaff(code: String): Staff {
        if (!CODE_PATTERN.matches(code)) throw ApiException(400, "Invalid code format")
        return staffRepository.findByCode(code) ?: throw ApiException(401, "Unauthorized")
    }

    private companion object {
        val CODE_PATTERN = Regex("^[A-Za-z0-9]{4}$")
    }
}

