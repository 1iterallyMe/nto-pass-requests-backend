package ru.nto.team

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

class ApiException(val status: Int, message: String) : RuntimeException(message)

class FieldErrors(val errors: Map<String, String>) : RuntimeException("Validation failed")

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun api(error: ApiException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(error.status).body(mapOf("error" to error.message.orEmpty()))

    @ExceptionHandler(FieldErrors::class)
    fun fieldErrors(error: FieldErrors): ResponseEntity<Map<String, Map<String, String>>> =
        ResponseEntity.badRequest().body(mapOf("errors" to error.errors))
}

