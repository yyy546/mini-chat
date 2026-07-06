package com.minichat.common.core.enums;

import lombok.Getter;

@Getter
public enum GenderEnum {
    UNKNOWN(0, "未知"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final Integer code;
    private final String text;

    GenderEnum(Integer code, String text) {
        this.code = code;
        this.text = text;
    }

    // 根据code获取text
    public static String getTextByCode(Integer code) {
        for (GenderEnum gender : values()) {
            if (gender.getCode().equals(code)) {
                return gender.getText();
            }
        }
        return UNKNOWN.getText();
    }

    // 根据text获取code
    public static Integer getCodeByText(String text) {
        for (GenderEnum gender : values()) {
            if (gender.getText().equals(text)) {
                return gender.getCode();
            }
        }
        return UNKNOWN.getCode();
    }
}
