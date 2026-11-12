-- Gives a transfer's reservation back to the daily limit counter; does nothing when the transfer holds none.
-- KEYS[1]: daily-limit:v1:{accountId}:{day}. ARGV: transferId.
-- Returns 1 when released, 0 when there was nothing to release.
local amount = redis.call('HGET', KEYS[1], ARGV[1])
if not amount then
    return 0
end
redis.call('HDEL', KEYS[1], ARGV[1])
redis.call('HINCRBY', KEYS[1], 'total', -tonumber(amount))
return 1
