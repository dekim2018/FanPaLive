package com.livebroadcast.fandom.domain.target;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 같은 대상이 표기만 다르게 두 번 등록되지 않도록 이름을 비교용 문자열로 바꾼다.
 * "Tottenham Hotspur", "tottenham-hotspur", " 토트넘 홋스퍼 " 처럼 공백, 대소문자, 기호 차이를 없앤다.
 */
public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String nfc = Normalizer.normalize(name, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(nfc.length());
        nfc.codePoints()
                .filter(cp -> Character.isLetterOrDigit(cp))
                .forEach(sb::appendCodePoint);
        return sb.toString();
    }
}
