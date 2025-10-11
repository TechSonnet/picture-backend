package com.sonnet.picturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sonnet.picturebackend.annotation.AuthCheck;
import com.sonnet.picturebackend.common.BaseResponse;
import com.sonnet.picturebackend.common.Constant;
import com.sonnet.picturebackend.common.DeleteRequest;
import com.sonnet.picturebackend.common.ResultUtils;
import com.sonnet.picturebackend.exception.BusinessException;
import com.sonnet.picturebackend.exception.ErrorCode;
import com.sonnet.picturebackend.model.dto.picture.*;
import com.sonnet.picturebackend.model.entry.PictureTagCategory;
import com.sonnet.picturebackend.model.vo.PictureVO;
import com.sonnet.picturebackend.model.entry.Picture;
import com.sonnet.picturebackend.model.entry.User;
import com.sonnet.picturebackend.service.PictureService;
import com.sonnet.picturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/v1/pic")
public class PictureController {


    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;


    @PostMapping("/upload")
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


    /**
     * 更新图片信息，供管理员使用
     * @param pictureUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    private BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                                HttpServletRequest request){
        // 基本参数校验
        if (pictureUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 调用服务更新图片
        boolean result = pictureService.updatePicture(pictureUpdateRequest, request);

        // 返回结果
        return ResultUtils.success(result);
    }

    /**
     * 编辑图片，提供给普通用户使用
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                             HttpServletRequest request){

        // 1. 基本校验
        if (pictureEditRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 2. 编辑图片
        boolean result = pictureService.editPicture(pictureEditRequest, request);

        // 4. 返回结果
        return ResultUtils.success(result);
    }

    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 1. 基本参数校验
        if (deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 2. 权限校验，仅用户和管理员可删除
        User loginUser = userService.getLoginUser(request);
        Picture picture = pictureService.getById(deleteRequest.getId());
        if (picture == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        if (!picture.getUserId().equals(loginUser.getId())
                && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除");
        }

        // 3. 删除图片
        boolean result = pictureService.removeById(deleteRequest.getId());

        // 4. 返回结果
        return ResultUtils.success(result);
    }

    /**
     * 获取图片标签和分类
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest,
                                               HttpServletRequest request) {
        // 1. 基本参数校验
        if (pictureReviewRequest == null || pictureReviewRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 2. 权限校验

        // 3. 进行审核
        User loginUser = userService.getLoginUser(request);
        boolean result = pictureService.doReviewPicture(pictureReviewRequest, loginUser);

        // 4. 返回结果
        return ResultUtils.success(result);
    }

}
