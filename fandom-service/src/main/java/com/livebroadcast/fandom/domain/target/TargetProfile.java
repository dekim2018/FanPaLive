package com.livebroadcast.fandom.domain.target;

import java.util.regex.Pattern;

/**
 * 응원 대상의 표시 정보. 신청서와 실제 대상이 같은 규칙으로 검증되도록 하나로 묶었다.
 * 구단 로고는 상표 문제로 쓰지 않고 약칭 + 대표 색 두 개로 배지를 만든다.
 */
public record TargetProfile(String name, String nameEn, String shortName, String colorPrimary,
                            String colorSecondary, String description) {

    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final String DEFAULT_PRIMARY = "#5B6474";
    private static final String DEFAULT_SECONDARY = "#FFFFFF";

    public TargetProfile {
        name = name == null ? null : name.strip();
        if (name == null || name.length() < 2 || name.length() > 40) {
            throw TargetErrors.invalidName();
        }
        nameEn = nameEn == null || nameEn.isBlank() ? null : nameEn.strip();
        if (nameEn != null && nameEn.length() > 60) {
            throw TargetErrors.invalidName();
        }
        shortName = shortName == null || shortName.isBlank() ? defaultShortName(name) : shortName.strip();
        if (shortName.isEmpty() || shortName.length() > 6) {
            throw TargetErrors.invalidShortName();
        }
        colorPrimary = color(colorPrimary, DEFAULT_PRIMARY);
        colorSecondary = color(colorSecondary, DEFAULT_SECONDARY);
        description = description == null ? "" : description.strip();
        if (description.length() > 300) {
            description = description.substring(0, 300);
        }
    }

    public String normalizedName() {
        return NameNormalizer.normalize(name);
    }

    private static String color(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        if (!COLOR.matcher(value.strip()).matches()) {
            throw TargetErrors.invalidColor();
        }
        return value.strip().toUpperCase();
    }

    private static String defaultShortName(String name) {
        String first = name.split("\\s+")[0];
        return first.length() > 6 ? first.substring(0, 6) : first;
    }
}
