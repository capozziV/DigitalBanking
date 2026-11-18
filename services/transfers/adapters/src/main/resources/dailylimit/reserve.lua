-- Reserves a transfer in the daily limit counter of an account and day, in one indivisible step.
-- KEYS[1]: daily-limit:v1:{accountId}:{day}, a hash with total, epoch and one field per transferId (cents).
-- ARGV: transferId, amount (cents), limit (cents), ttlSeconds, epoch (the one in PostgreSQL).
-- Returns 1 when reserved (now or before: idempotent), 0 when the limit would be passed, -1 when the key is missing or
-- was seeded with another epoch, or by another Redis instance (run_id), than the ones that answer now (the caller
-- rebuilds it from PostgreSQL and tries again).
if redis.call('EXISTS', KEYS[1]) == 0 then
    return -1
end
local run_id = string.match(redis.call('INFO', 'server'), 'run_id:(%x+)')
if redis.call('HGET', KEYS[1], 'epoch') ~= ARGV[5] or redis.call('HGET', KEYS[1], 'instance') ~= run_id then
    return -1
end
if redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[4])
    return 1
end
local total = tonumber(redis.call('HGET', KEYS[1], 'total') or '0')
local amount = tonumber(ARGV[2])
if total + amount > tonumber(ARGV[3]) then
    return 0
end
redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
redis.call('HINCRBY', KEYS[1], 'total', ARGV[2])
redis.call('EXPIRE', KEYS[1], ARGV[4])
return 1
