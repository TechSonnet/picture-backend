package com.sonnet.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sonnet.picturebackend.model.dto.picture.*;
import com.sonnet.picturebackend.model.vo.PictureVO;
import com.sonnet.picturebackend.model.entry.Picture;
import com.sonnet.picturebackend.model.entry.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sonnet.picturebackend.model.vo.UploadPictureResult;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author Administrator
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-09-30 19:00:24
*/
public interface PictureService extends IService<Picture> {

    Wrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);


    Page<PictureVO> getPictureVOList(Page<Picture> picturePage);

    boolean doReviewPicture(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);

    boolean editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request);

    boolean updatePicture(PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request);

    void validatePicture(Picture picture);

    boolean doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);


    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);
}
