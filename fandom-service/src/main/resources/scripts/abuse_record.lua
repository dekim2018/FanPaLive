-- 오늘 이 대상에 하트를 준 계정을 기기, IP 별 집합에 넣고 서로 다른 계정 수를 돌려준다.
-- KEYS[1] 기기 집합, KEYS[2] IP 집합
-- ARGV[1] 사용자 ID, ARGV[2] 만료(초), ARGV[3] 기기 신호 있음(1/0), ARGV[4] IP 신호 있음(1/0)
local device = 1
local ip = 1
if ARGV[3] == '1' then
    redis.call('SADD', KEYS[1], ARGV[1])
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
    device = redis.call('SCARD', KEYS[1])
end
if ARGV[4] == '1' then
    redis.call('SADD', KEYS[2], ARGV[1])
    redis.call('EXPIRE', KEYS[2], tonumber(ARGV[2]))
    ip = redis.call('SCARD', KEYS[2])
end
return {device, ip}
