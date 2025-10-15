package com.sonnet.picturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {


    /**
     * 图片 id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String fileUrl;

    private static final long serialVersionUID = 1L;
}
