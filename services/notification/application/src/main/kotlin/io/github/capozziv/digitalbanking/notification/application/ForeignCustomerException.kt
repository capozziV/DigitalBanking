package io.github.capozziv.digitalbanking.notification.application

/**
 * The caller asked for the notifications of a customer they may not read (another customer, or a caller that is
 * not a user at all). Answered like a notification that does not exist, so nothing is revealed.
 */
class ForeignCustomerException : RuntimeException("the caller may not access the notifications of that customer")
