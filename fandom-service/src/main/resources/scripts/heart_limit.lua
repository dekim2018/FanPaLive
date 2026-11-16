-- 하트 일일 한도 검사와 차감을 한 번에(원자적으로) 한다. 연타나 동시 요청에도 한도를 넘지 않는다.
-- KEYS[1] 사용자의 오늘 합계, KEYS[2] 사용자가 이 대상에 준 오늘 합계
-- ARGV[1] 줄 개수, ARGV[2] 하루 한도, ARGV[3] 대상당 한도, ARGV[4] 만료(초)
-- 반환: {허용 여부(1/0), 남은 하루 한도, 남은 대상 한도}
local amount = tonumber(ARGV[1])
local dailyMax = tonumber(ARGV[2])
local perTargetMax = tonumber(ARGV[3])
local total = tonumber(redis.call('GET', KEYS[1]) or '0')
local perTarget = tonumber(redis.call('GET', KEYS[2]) or '0')
if total + amount > dailyMax or perTarget + amount > perTargetMax then
    return {0, dailyMax - total, perTargetMax - perTarget}
end
redis.call('INCRBY', KEYS[1], amount)
redis.call('INCRBY', KEYS[2], amount)
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4]))
redis.call('EXPIRE', KEYS[2], tonumber(ARGV[4]))
return {1, dailyMax - total - amount, perTargetMax - perTarget - amount}
