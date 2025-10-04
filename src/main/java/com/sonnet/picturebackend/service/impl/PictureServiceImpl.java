package com.sonnet.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sonnet.picturebackend.exception.BusinessException;
import com.sonnet.picturebackend.exception.ErrorCode;
import com.sonnet.picturebackend.manager.FileManager;
import com.sonnet.picturebackend.mapper.PictureMapper;
import com.sonnet.picturebackend.model.dto.PictureQueryRequest;
import com.sonnet.picturebackend.model.dto.PictureUploadRequest;
import com.sonnet.picturebackend.model.vo.PictureVO;
import com.sonnet.picturebackend.model.dto.UploadPictureResult;
import com.sonnet.picturebackend.model.entry.Picture;
import com.sonnet.picturebackend.model.entry.User;
import com.sonnet.picturebackend.model.vo.UserVO;
import com.sonnet.picturebackend.service.PictureService;
import com.sonnet.picturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-09-30 18:50:08
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    private final FileManager fileManager;
    private final UserService userService;

    public PictureServiceImpl(FileManager fileManager, UserService userService) {
        this.fileManager = fileManager;
        this.userService = userService;
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
        // 1. 详细校验
        if (multipartFile == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "params is null");
        }
        if (loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "user is not login");
        }

        // 2. 判断是新增图片还是更新图片， 更新需判断图片是否存在
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
        // 3. 上传图片，获取上传后的信息
        // a. 构造存储路径并上云
        String uploadPathPrefix = String.format("public/%s", loginUser.getId()); // 图片均存放在 public 目录下，且每个用户一个子目录目录
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        // b. 构造存入数据库的信息并存入
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());

        // 4. 如果为更新操作，更新编辑时间
        if (pictureId != null){
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean res = this.saveOrUpdate(picture);
        if (!res){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "save or update picture failed");
        }

        // 5. 返回结果
        return PictureVO.objToVo(picture);
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

}




