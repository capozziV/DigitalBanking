package io.github.capozziv.digitalbanking.notification.adapters.web

/** A field that failed validation, as listed in `errors` of the validation-error problem. */
data class InvalidField(
    val field: String,
    val message: String,
)

class InvalidRequestException(
    val errors: List<InvalidField>,
) : RuntimeException("invalid request: ${errors.joinToString { it.field }}")
