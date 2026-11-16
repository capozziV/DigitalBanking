package io.github.capozziv.digitalbanking.customer.adapters.web

/** Texts shared by the OpenAPI annotations of the controllers. */
internal object ApiDocs {
    const val PROBLEMS = "/problems/"
    const val PROBLEM_JSON = "application/problem+json"

    const val NOT_FOUND = "/problems/customer-not-found"
    const val VALIDATION_ERROR = "/problems/validation-error: invalid path or body fields, listed in `errors`"
}
