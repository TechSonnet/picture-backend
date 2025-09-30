package com.sonnet.picturebackend.service;

import com.sonnet.picturebackend.model.dto.PictureUploadRequest;
import com.sonnet.picturebackend.model.dto.PictureVO;
import com.sonnet.picturebackend.model.entry.Picture;
import com.sonnet.picturebackend.model.entry.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
* @author Administrator
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-09-30 19:00:24
*/
public interface PictureService extends IService<Picture> {
    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);
}
