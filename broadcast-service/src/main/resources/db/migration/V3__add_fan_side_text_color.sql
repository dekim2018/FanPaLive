-- 구단별 2색(진한 배경색 + 연한 글자색)을 방송과 녹화에도 복사해 둔다.
ALTER TABLE broadcasts ADD COLUMN target_text_color VARCHAR(7);
ALTER TABLE recordings ADD COLUMN target_text_color VARCHAR(7);

-- 이미 만들어진 KBO 편파방과 녹화는 새 팔레트로 맞춘다(구단 이름이 고정이라 이름으로 맞춘다).
UPDATE broadcasts b SET target_color = p.bg, target_text_color = p.fg
FROM (VALUES
    ('두산 베어스', '#131230', '#FFFFFF'), ('LG 트윈스', '#A0073F', '#FFE3EC'), ('KIA 타이거즈', '#B8001F', '#FFFFFF'),
    ('삼성 라이온즈', '#0B4A9A', '#D6E8FF'), ('SSG 랜더스', '#A80F27', '#FFD25A'), ('롯데 자이언츠', '#0D2147', '#F48FAA'),
    ('한화 이글스', '#B84A00', '#FFF1E3'), ('NC 다이노스', '#1F3A66', '#E2C99A'), ('KT 위즈', '#141414', '#FF5A5F'),
    ('키움 히어로즈', '#5A0A1C', '#E9C28E')
) AS p(name, bg, fg)
WHERE b.league_code = 'KBO' AND b.target_name = p.name;

UPDATE recordings r SET target_color = p.bg, target_text_color = p.fg
FROM (VALUES
    ('두산 베어스', '#131230', '#FFFFFF'), ('LG 트윈스', '#A0073F', '#FFE3EC'), ('KIA 타이거즈', '#B8001F', '#FFFFFF'),
    ('삼성 라이온즈', '#0B4A9A', '#D6E8FF'), ('SSG 랜더스', '#A80F27', '#FFD25A'), ('롯데 자이언츠', '#0D2147', '#F48FAA'),
    ('한화 이글스', '#B84A00', '#FFF1E3'), ('NC 다이노스', '#1F3A66', '#E2C99A'), ('KT 위즈', '#141414', '#FF5A5F'),
    ('키움 히어로즈', '#5A0A1C', '#E9C28E')
) AS p(name, bg, fg)
WHERE r.league_code = 'KBO' AND r.target_name = p.name;
