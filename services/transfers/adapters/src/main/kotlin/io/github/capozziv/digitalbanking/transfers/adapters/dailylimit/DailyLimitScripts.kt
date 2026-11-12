package io.github.capozziv.digitalbanking.transfers.adapters.dailylimit

import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.script.RedisScript

/** The Lua scripts of the daily limit counter, from `src/main/resources/dailylimit/`; each one returns an integer. */
class DailyLimitScripts {
    /** 1 reserved (now or before), 0 exceeded, -1 key missing. */
    val reserve: RedisScript<Long> = load("reserve.lua")

    /** 1 released, 0 nothing to release. */
    val release: RedisScript<Long> = load("release.lua")

    /** 1 seeded, 0 the key already existed. */
    val seed: RedisScript<Long> = load("seed.lua")

    // The boxed class: Spring Data Redis reads a Lua integer reply only for java.lang.Long, not for the primitive.
    private fun load(name: String): RedisScript<Long> = RedisScript.of(ClassPathResource("dailylimit/$name"), Long::class.javaObjectType)
}
