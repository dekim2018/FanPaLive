-- 순위표 점수를 delta 만큼 바꾸고, "이 점수에 도달한 시각"을 함께 다시 기록한다.
-- encoded = score * base + (base - 1 - 시즌 경과 초). 같은 점수면 먼저 도달한 쪽이 크다.
-- KEYS[1] 순위표(ZSET)
-- ARGV[1] 대상 ID, ARGV[2] delta, ARGV[3] 시즌 경과 초, ARGV[4] base, ARGV[5] 최대 점수
-- 순위표가 아직 없으면(기간이 막 바뀜) 아무것도 하지 않는다. 다음 재계산이 DB 기준으로 만든다.
if redis.call('EXISTS', KEYS[1]) == 0 then
    return -1
end
local base = tonumber(ARGV[4])
local maxScore = tonumber(ARGV[5])
local score = 0
local current = redis.call('ZSCORE', KEYS[1], ARGV[1])
if current then
    local v = tonumber(current)
    -- v / base 를 바로 내림하면 큰 값에서 반올림 오차로 다음 정수가 될 수 있어 fmod 로 나머지를 정확히 뺀다.
    local remainder = math.fmod(v, base)
    score = (v - remainder) / base
end
score = score + tonumber(ARGV[2])
if score < 0 then score = 0 end
if score > maxScore then score = maxScore end
local elapsed = tonumber(ARGV[3])
if elapsed < 0 then elapsed = 0 end
if elapsed > base - 1 then elapsed = base - 1 end
local encoded = score * base + (base - 1 - elapsed)
-- tostring 은 유효숫자 14자리라 정밀도가 깨지므로 정수 형식으로 넘긴다.
redis.call('ZADD', KEYS[1], string.format('%.0f', encoded), ARGV[1])
return 1
