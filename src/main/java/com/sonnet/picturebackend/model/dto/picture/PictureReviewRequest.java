package com.sonnet.picturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureReviewRequest implements Serializable {

    // reviewId 和 reviewTime 由系统自动填入

    /**
     * id
     */
    private Long id;
    /**
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;


    private static final long serialVersionUID = 1L;
}
