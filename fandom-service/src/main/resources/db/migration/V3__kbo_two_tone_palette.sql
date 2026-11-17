-- KBO 구단 2색 팔레트: color_primary = 진한 배경색, color_secondary = 연한 글자색.
-- 같은 계열(두산과 롯데 남색, KIA와 SSG 빨강)은 글자색으로 구분한다. 모든 조합은 명도 대비 4.5:1 이상.
UPDATE fan_targets t SET color_primary = p.bg, color_secondary = p.fg
FROM leagues l, (VALUES
    ('두산 베어스', '#131230', '#FFFFFF'), ('LG 트윈스', '#A0073F', '#FFE3EC'), ('KIA 타이거즈', '#B8001F', '#FFFFFF'),
    ('삼성 라이온즈', '#0B4A9A', '#D6E8FF'), ('SSG 랜더스', '#A80F27', '#FFD25A'), ('롯데 자이언츠', '#0D2147', '#F48FAA'),
    ('한화 이글스', '#B84A00', '#FFF1E3'), ('NC 다이노스', '#1F3A66', '#E2C99A'), ('KT 위즈', '#141414', '#FF5A5F'),
    ('키움 히어로즈', '#5A0A1C', '#E9C28E')
) AS p(name, bg, fg)
WHERE t.league_id = l.id AND l.code = 'KBO' AND t.name = p.name;
