package com.livebroadcast.auth.app;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 평가용 테스트 계정. 서버 기동 시 자동 생성되고, 로그인 화면의 원클릭 로그인에 사용된다.
 * 원클릭 로그인은 아이디만으로 로그인되므로 기본값은 꺼짐이고, 평가 기간에만 켠다.
 * 비밀번호를 정하지 않으면 기동할 때마다 무작위 비밀번호를 써서 아이디/비밀번호 로그인은 막힌다.
 */
@ConfigurationProperties(prefix = "app.test-accounts")
public record TestAccountProperties(boolean enabled, String password, List<Account> accounts) {

    public TestAccountProperties {
        accounts = accounts == null ? List.of() : List.copyOf(accounts);
    }

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }

    public boolean isTestAccount(String username) {
        return enabled && accounts.stream().anyMatch(a -> a.username().equals(username));
    }

    /**
     * @param role USER(기본) 또는 ADMIN. ADMIN 계정으로 팀/선수 신청 승인과 경기 등록을 확인한다.
     * 생성자가 둘 이상이면 설정 바인딩이 생성자를 고르지 못해 값이 버려지므로, 기본값은 compact 생성자에서 채운다.
     */
    public record Account(String username, String nickname, String role) {

        public Account {
            role = role == null || role.isBlank() ? "USER" : role;
        }
    }
}
