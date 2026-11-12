-- Creates the daily limit counter from the reservations in PostgreSQL, only when the key does not exist.
-- KEYS[1]: daily-limit:v1:{accountId}:{day}. ARGV: epoch, ttlSeconds, then transferId and amount (cents) pairs.
-- Returns 1 when seeded, 0 when the key already existed.
if redis.call('EXISTS', KEYS[1]) == 1 then
    return 0
end
redis.call('HSET', KEYS[1], 'epoch', ARGV[1], 'total', 0)
for i = 3, #ARGV, 2 do
    redis.call('HSET', KEYS[1], ARGV[i], ARGV[i + 1])
    redis.call('HINCRBY', KEYS[1], 'total', ARGV[i + 1])
end
redis.call('EXPIRE', KEYS[1], ARGV[2])
return 1
