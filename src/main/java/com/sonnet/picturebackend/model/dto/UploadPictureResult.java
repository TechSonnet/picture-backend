package com.sonnet.picturebackend.model.dto;

import lombok.Data;

/**
 * 接收图片信息解析的类
 * 注意，这个类承接来自数据万象的解析结果，而非前端
 * 再次加深了认识，dto 不仅接收来自前端的信息，而是来自外界的结果
 */
@Data
public class UploadPictureResult {

    /**
     * 图片地址
     */
    private String url;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 文件体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private int picWidth;

    /**
     * 图片高度
     */
    private int picHeight;

    /**
     * 图片宽高比
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

}

