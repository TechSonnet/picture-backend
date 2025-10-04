package com.sonnet.picturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sonnet.picturebackend.annotation.AuthCheck;
import com.sonnet.picturebackend.common.BaseResponse;
import com.sonnet.picturebackend.common.Constant;
import com.sonnet.picturebackend.common.ResultUtils;
import com.sonnet.picturebackend.exception.BusinessException;
import com.sonnet.picturebackend.exception.ErrorCode;
import com.sonnet.picturebackend.model.dto.PictureQueryRequest;
import com.sonnet.picturebackend.model.dto.PictureUploadRequest;
import com.sonnet.picturebackend.model.vo.PictureVO;
import com.sonnet.picturebackend.model.entry.Picture;
import com.sonnet.picturebackend.model.entry.User;
import com.sonnet.picturebackend.service.PictureService;
import com.sonnet.picturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/pic")
public class PictureController {


    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;


    @PostMapping("/upload")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 同过 ID 获取图片完整信息
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/get")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(Long id, HttpServletRequest request) {
        // 1. 基础校验
        if (id == null || id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 2. 数据库查询图片
        Picture picture = pictureService.getById(id);
        if (picture == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        // 3. 返回结果
        return ResultUtils.success(picture);
    }


    /**
     * 通过 ID 获取图片脱敏信息
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id, HttpServletRequest request) {
        // 1. 基本参数校验
        if (id == null || id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 2. 查询数据并转换
        Picture picture = pictureService.getById(id);
        if (picture == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        PictureVO pictureVO = PictureVO.objToVo(picture);

        // 3. 返回结果
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片列表
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        // 1. 基本校验
        if (pictureQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 2. 数据库查询图片
        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();
        if (current <= 0 || pageSize <= 0){
            current = 1L;
            pageSize = 10L;
        }
        Page<Picture> picturePage = pictureService.page(
                new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest)
        ); // 仔细学习下这种分页查询的写法，不是很熟悉

        // 3. 返回结果
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（脱敏）
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {

        // 1. 校验逻辑
        if (pictureQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 2. 获取图片列表
        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest)
        );

        // 3. 返回结果
        return ResultUtils.success(pictureService.getPictureVOList(picturePage));
    }

//    @PostMapping("/deit")
//    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
//                                             HttpServletRequest request){
//
//        // 1. 基本校验
//        if (pictureEditRequest == null){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
//        }
//
//        // 2. vo ->  entity
//        Picture picture = PictureVO.voToObj(pictureEditRequest);
//        // 将 list 转换成 json，这个与具体类的设计有过
//        // 不过，此处将 vo 中的 tags 设置为列表，这个经验可以学习
//        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
//
//        // 3. 编辑并更新
//        // a. 权限校验
//        User loginUser = userService.getLoginUser(request);
//        if (loginUser == null){
//            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
//        }
//        Picture oldPicture = pictureService.getById(picture.getId());
//        if (oldPicture == null){
//            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
//        }
//        // b. 仅本人和管理员可更新
//        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
//        // c. 执行更新操作
//
//
//        // 4. 返回结果
//    }

}
