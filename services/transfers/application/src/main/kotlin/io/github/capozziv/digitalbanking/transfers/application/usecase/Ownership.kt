package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.security.Caller
import java.util.UUID

/**
 * True when the caller may see or operate what belongs to [customerId]: an admin always, a customer (or an admin
 * that is that customer) only its own. A null owner (a transfer from before stage 7) is visible to admins only.
 * The answer for anyone else is "not found", never "forbidden", so the resource does not reveal it exists.
 */
internal fun Caller.mayAccess(customerId: UUID?): Boolean = this is Caller.Admin || (customerId != null && actsAsCustomer(customerId))
