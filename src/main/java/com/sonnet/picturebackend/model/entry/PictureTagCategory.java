package com.sonnet.picturebackend.model.entry;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureTagCategory implements Serializable {
    /**
     * 标签
     */
    private List<String> tagList;
    /**
     * 分类
     */
    private List<String> categoryList;

    private static final long serialVersionUID = 1L;
}
