-- 관리자(팀/선수 신청 승인, 경기 등록) 구분을 위한 역할 컬럼. 기존 회원은 모두 USER
ALTER TABLE users ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));
