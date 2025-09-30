package com.sonnet.picturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sonnet.picturebackend.exception.BusinessException;
import com.sonnet.picturebackend.exception.ErrorCode;
import com.sonnet.picturebackend.mapper.PictureMapper;
import com.sonnet.picturebackend.model.dto.PictureUploadRequest;
import com.sonnet.picturebackend.model.dto.PictureVO;
import com.sonnet.picturebackend.model.entry.Picture;
import com.sonnet.picturebackend.model.entry.User;
import com.sonnet.picturebackend.service.PictureService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
* @author Administrator
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-09-30 18:50:08
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

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
        if (pictureUploadRequest != null){
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
        // a. 上云
        // b. 构造存入数据库的信息并存入
        // 4. 更新操作，更新编辑时间
        // 5. 返回结果
        return null;
    }

}




