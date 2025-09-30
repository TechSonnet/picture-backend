package com.sonnet.picturebackend.controller;

import com.sonnet.picturebackend.common.BaseResponse;
import com.sonnet.picturebackend.common.ResultUtils;
import com.sonnet.picturebackend.model.dto.PictureUploadRequest;
import com.sonnet.picturebackend.model.dto.PictureVO;
import com.sonnet.picturebackend.model.entry.User;
import com.sonnet.picturebackend.service.PictureService;
import com.sonnet.picturebackend.service.UserService;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController("/v1/pic")
public class PictureController {


    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;


    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

}
