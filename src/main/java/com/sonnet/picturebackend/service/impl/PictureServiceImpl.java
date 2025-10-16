package com.sonnet.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.sonnet.picturebackend.config.CosClientConfig;
import com.sonnet.picturebackend.exception.BusinessException;
import com.sonnet.picturebackend.exception.ErrorCode;
import com.sonnet.picturebackend.exception.ThrowUtils;
import com.sonnet.picturebackend.manager.CosManager;
import com.sonnet.picturebackend.manager.FileManager;
import com.sonnet.picturebackend.mapper.PictureMapper;
import com.sonnet.picturebackend.model.dto.picture.*;
import com.sonnet.picturebackend.model.enums.PictureReviewStatusEnum;
import com.sonnet.picturebackend.model.vo.PictureVO;
import com.sonnet.picturebackend.model.entry.Picture;
import com.sonnet.picturebackend.model.entry.User;
import com.sonnet.picturebackend.model.vo.UploadPictureResult;
import com.sonnet.picturebackend.model.vo.UserVO;
import com.sonnet.picturebackend.service.PictureService;
import com.sonnet.picturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-09-30 18:50:08
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileManager fileManager;
    @Resource
    private UserService userService;
    @Resource
    private CosManager cosManager;
    @Resource
    private CosClientConfig cosClientConfig;


    /**
     * 填充默认审核信息，图片被创建或更新时，根据用户角色自动填充审核信息
     * @param picture
     * @param loginUser
     */
    public void fillReviewParams(Picture picture, User loginUser) {
        // 管理员，自动填入审核信息
        if (userService.isAdmin(loginUser)){
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        }

        // 用户，置为待审核状态
        if (!userService.isAdmin(loginUser)){
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public boolean editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        // 1. 详细参数校验
        if (pictureEditRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // vo ->  entity
        Picture picture = new Picture();
        fillReviewParams(picture, userService.getLoginUser(request));
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // note: 将 list 转换成 json，这个与具体类的设计有关
        // note:不过，此处将 vo 中的 tags 设置为列表，这个经验可以学习
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        picture.setEditTime(new Date());

        // 编辑并更新
        // a. 权限校验
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Picture oldPicture = this.getById(picture.getId());
        if (oldPicture == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // b. 仅本人和管理员可更新
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 自动填充审核信息
        fillReviewParams(picture, loginUser);

        // 执行更新操作
        boolean result = this.updateById(picture);
        if (!result){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败");
        }

        // 返回结果
        return result;
    }

    @Override
    public boolean updatePicture(PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {

        // 详细参数校验
        if (pictureUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "params is null");
        }

        // 数据转换
        // a. DTO 转换为实体类
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // b. DTO 的 list 转换为 String
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));

        // 自动填充检验信息
        fillReviewParams(picture, userService.getLoginUser(request));

        // 校验图片是否存在
        validatePicture(picture);
        boolean exists = this.lambdaQuery()
                .eq(Picture::getId, picture.getId())
                .exists();
        if (!exists){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "picture is not exists");
        }

        // 操作数据库
        boolean result = this.updateById(picture);
        if (!result){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "update picture failed");
        }

        return true;
    }

    /**
     * 获取查询条件
     * @param pictureQueryRequest
     * @return 返回一个封装好的 Wrapper
     */
    // 就这种封装思想，可以注意学习下
    @Override
    public Wrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        // 1. 基本校验
        if (pictureQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "params is null");
        }

        // 2. 封装查询条件
        // a. 从对象中取值
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        // b. 构造 QueryWrapper
        // 从多字段中搜索(典型的动态构造查询条件)
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id) && id > 0, "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId) && id > 0, "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth) && id > 0, "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight) && id > 0, "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize) && id > 0, "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale) && id > 0, "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus) && id > 0, "reviewStatus", reviewStatus);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(
                StrUtil.isNotEmpty(sortField), // 查询条件是否生效
                sortOrder.equals("ascend"), // 是否升序
                sortField // 排序字段
        );


        // 3. 封装返回结果
        return queryWrapper;
    }

    @Override
    public Page<PictureVO> getPictureVOList(Page<Picture> picturePage) {
        // 1. 基本校验
        if (CollUtil.isEmpty(picturePage.getRecords())){
            return null;
        }

        // 2. 脱敏，注意这里需要将 PictureVO 中的 User 也脱敏
        List<Picture> records = picturePage.getRecords();
        List<PictureVO> voList = records.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        // 这样写没有问题，但是效率非常低，每次都需要查询数据库
//        for (PictureVO vo : voList) {
//            User user = userService.getById(vo.getUserId());
//            UserVO userVO = UserVO.objToVo(user);
//            vo.setUser(userVO);
//        }
        // 如下写法，仅查询一次数据库
        Set<Long> userIdSet = voList.stream()
                .map(PictureVO::getUserId)
                .collect(Collectors.toSet());
        List<User> users = userService.listByIds(userIdSet);
        Map<Long, User> userMap = users.stream().
                collect(Collectors.toMap(User::getId, user -> user));
        for (PictureVO vo : voList) {
            User user = userMap.get(vo.getUserId());
            UserVO userVO = UserVO.objToVo(user);
            vo.setUser(userVO);
        }
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize());
        pictureVOPage.setRecords(voList);

        // 3. 返回结果
        return pictureVOPage;
    }

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean doReviewPicture(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1. 参数校验
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum anEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || anEnum == null ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 校验图片是否存在
        Picture picture = this.getById(pictureReviewRequest.getId());
        if (picture == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        if (picture.getReviewStatus().equals(pictureReviewRequest.getReviewStatus())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "重复操作");
        }

        // 3. 修改图片状态
        // 4. 封装返回结果
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        return this.updateById(updatePicture);

    }


    /**
     * 校验图片
     * @param picture
     */
    @Override
    public void validatePicture(Picture picture) {
        // 获取参数
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();

        // 对参数进行校验
        if (id == null || id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片id不能为空");
        }
        if (StrUtil.isNotBlank(url)){
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "图片url不能超过1024个字符");
        }
        if (StrUtil.isNotBlank(introduction)){
            ThrowUtils.throwIf(introduction.length() > 1024, ErrorCode.PARAMS_ERROR, "图片简介不能超过1024个字符");
        }
    }

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {

        // 详细参数校验

        // 存在性判断：图片
        Picture oldPicture = this.lambdaQuery()
                .ge(Picture::getId, pictureReviewRequest.getId())
                .getEntity();
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 判断图片状态是已符合要求
        if (oldPicture.getReviewStatus().equals(PictureReviewStatusEnum.REVIEWING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "勿重复审核");
        }

        // 修改数据库，更新对应用户权限
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "审核失败");

        // 返回结果
        return true;
    }

    /**
     * 上传图片
     * @param multipartFile
     * @param pictureUploadRequest
     * @param loginUser
     * @return 脱敏图片信息
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 详细参数校验
        if (multipartFile == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "params is null");
        }
        if (loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "user is not login");
        }

        // 仅管理员或者本人可以进行上传操作
        Picture oldPicture = this.getById(pictureUploadRequest.getId());
        if (!userService.isAdmin(loginUser) && !loginUser.getId().equals(oldPicture.getUserId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "no auth to upload picture");
        }

        // 判断是新增图片还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest.getId() != null){
            pictureId = pictureUploadRequest.getId();
        }
        if (pictureId != null){
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            if (!exists){
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "picture is not exists");
            }
        }

        // 自动填充检验参数
        fillReviewParams(oldPicture, loginUser);

        // 上传图片，获取上传后的信息
        // a. 构造存储路径并上云
        String uploadPathPrefix = String.format("public/%s", loginUser.getId()); // 图片均存放在 public 目录下，且每个用户一个子目录目录
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        // b. 构造存入数据库的信息并存入
        Picture picture = new Picture();
        fillReviewParams(picture, loginUser); // 填写审核参数
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());

        // 如果为更新操作，更新编辑时间
        if (pictureId != null){
            // a. 仅本人和管理员可以编辑
            if (!userService.isAdmin(loginUser)
                    && !picture.getUserId().equals(loginUser.getId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "no auth to edit picture");
            }
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean res = this.saveOrUpdate(picture);
        if (!res){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "save or update picture failed");
        }

        // 返回结果
        return PictureVO.objToVo(picture);
    }

    /**
     * 通过 URL 上传图片
     * @param pictureUploadRequest
     * @param currentUser
     * @return
     */
    @Override
    public PictureVO uploadPictureByUrl(PictureUploadRequest pictureUploadRequest, User currentUser) {

        // 校验，对接受到的参数进行严格校验
        String fileUrl = pictureUploadRequest.getFileUrl();
        validatePicture(fileUrl);

        // 调用通用服务，实现图片上传
        /// 一定要注意这里，这里调用了 fileManager 中的函数
        ///  这是因为，这其实是一个较为底层且通用的能力，不必要在此处详细的展开，可以抽取出来
        UploadPictureResult uploadPictureResult = fileManager.uploadPictureByUrl(fileUrl, currentUser);

        // 归属，构造对应的图片入库信息，存储到数据库
        Picture picture = new Picture();
        fillReviewParams(picture, currentUser); // 填写审核参数
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(currentUser.getId());
        boolean res = this.saveOrUpdate(picture);
        if (!res){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "save or update picture failed");
        }

        PictureVO pictureVO = PictureVO.objToVo(picture);

        return pictureVO;
    }



    /**
     * 通过校验图片
     * @param fileUrl
     */
    private void validatePicture(String fileUrl) {

        // 校验 URL 基本信息
        if(StrUtil.isBlank(fileUrl)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "fileUrl can not be null");
        }


        // 校验 URL 协议
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "fileUrl is invalid");
        }

        // 发送 HEAD 请求验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (response.getStatus() != HttpStatus.HTTP_OK){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "fileUrl is invalid");
            }

            // 校验文件类型
//            String contentType = response.header("Content-Type");
//            if (StrUtil.isNotBlank(contentType)){
//                final List<String> ALLOW_CONTENT_TYPE = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/jpg", "image/webp");
//                if (!ALLOW_CONTENT_TYPE.contains(contentType)){
//                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "fileUrl is invalid");
//                }
//            }

            // 检验文件大小
            long contentLength = response.contentLength();
            final long TWO_MB = 1024 * 1024 * 2;
            if (contentLength > TWO_MB){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
            }

        } catch (Exception e) {
            log.error("validate picture error", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "validate picture error");
        } finally {
            if (response != null){
                response.close();
            }
        }

    }


}




