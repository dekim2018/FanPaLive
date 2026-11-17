-- 종목, 리그, KBO 10구단(공식 대상) 시드.
-- 구단 로고는 상표 문제로 쓰지 않고 약칭 + 대표 색으로 배지를 만든다. 색은 팬파라이브가 정한 배지 색이다.

INSERT INTO sports (code, name, kind, sort_order) VALUES
    ('BASEBALL', '야구', 'TEAM', 1),
    ('FOOTBALL', '축구', 'TEAM', 2),
    ('BASKETBALL', '농구', 'TEAM', 3),
    ('TENNIS', '테니스', 'INDIVIDUAL', 4);

INSERT INTO leagues (sport_code, code, name, country, open_for_request, sort_order) VALUES
    ('BASEBALL', 'KBO', 'KBO 리그', '대한민국', FALSE, 1),
    ('BASEBALL', 'MLB', 'MLB', '미국', TRUE, 2),
    ('FOOTBALL', 'EPL', '프리미어리그', '잉글랜드', TRUE, 3),
    ('FOOTBALL', 'LALIGA', '라리가', '스페인', TRUE, 4),
    ('FOOTBALL', 'BUNDESLIGA', '분데스리가', '독일', TRUE, 5),
    ('FOOTBALL', 'SERIEA', '세리에A', '이탈리아', TRUE, 6),
    ('FOOTBALL', 'KLEAGUE', 'K리그1', '대한민국', TRUE, 7),
    ('BASKETBALL', 'NBA', 'NBA', '미국', TRUE, 8),
    ('TENNIS', 'ATP', 'ATP 투어', '국제', TRUE, 9),
    ('TENNIS', 'WTA', 'WTA 투어', '국제', TRUE, 10);

INSERT INTO fan_targets (league_id, kind, name, name_en, normalized_name, short_name, color_primary, color_secondary,
                         description, status, official, created_at)
SELECT l.id, 'TEAM', t.name, t.name_en, t.normalized_name, t.short_name, t.color_primary, t.color_secondary,
       t.description, 'ACTIVE', TRUE, NOW()
FROM leagues l
         CROSS JOIN (VALUES
    ('두산 베어스', 'Doosan Bears', '두산베어스', '두산', '#131230', '#ED1C24', '서울 잠실을 홈으로 쓰는 구단'),
    ('LG 트윈스', 'LG Twins', 'lg트윈스', 'LG', '#C30452', '#000000', '서울 잠실을 홈으로 쓰는 구단'),
    ('KIA 타이거즈', 'KIA Tigers', 'kia타이거즈', 'KIA', '#EA0029', '#06141F', '광주를 홈으로 쓰는 구단'),
    ('삼성 라이온즈', 'Samsung Lions', '삼성라이온즈', '삼성', '#074CA1', '#FFFFFF', '대구를 홈으로 쓰는 구단'),
    ('롯데 자이언츠', 'Lotte Giants', '롯데자이언츠', '롯데', '#041E42', '#D00F31', '부산을 홈으로 쓰는 구단'),
    ('한화 이글스', 'Hanwha Eagles', '한화이글스', '한화', '#FF6600', '#000000', '대전을 홈으로 쓰는 구단'),
    ('SSG 랜더스', 'SSG Landers', 'ssg랜더스', 'SSG', '#CE0E2D', '#FFB81C', '인천을 홈으로 쓰는 구단'),
    ('NC 다이노스', 'NC Dinos', 'nc다이노스', 'NC', '#315288', '#AF917B', '창원을 홈으로 쓰는 구단'),
    ('KT 위즈', 'KT Wiz', 'kt위즈', 'KT', '#000000', '#EB1C24', '수원을 홈으로 쓰는 구단'),
    ('키움 히어로즈', 'Kiwoom Heroes', '키움히어로즈', '키움', '#570514', '#B07F4A', '서울 고척을 홈으로 쓰는 구단')
) AS t(name, name_en, normalized_name, short_name, color_primary, color_secondary, description)
WHERE l.code = 'KBO';

INSERT INTO target_stats (target_id, updated_at)
SELECT id, NOW() FROM fan_targets;
