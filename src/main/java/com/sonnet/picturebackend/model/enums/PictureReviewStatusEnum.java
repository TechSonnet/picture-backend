package com.sonnet.picturebackend.model.enums;

import lombok.Getter;

@Getter
public enum PictureReviewStatusEnum {
    REVIEWING("审核中", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);

    private final String text;
    private final Integer value;

    PictureReviewStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举，这是枚举类必备方法
     *
     * @param value
     * @return
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value) {
        for (PictureReviewStatusEnum anEnum : PictureReviewStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
