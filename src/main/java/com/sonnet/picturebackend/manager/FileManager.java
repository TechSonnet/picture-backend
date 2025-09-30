package com.sonnet.picturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.sonnet.picturebackend.config.CosClientConfig;
import com.sonnet.picturebackend.exception.BusinessException;
import com.sonnet.picturebackend.exception.ErrorCode;
import com.sonnet.picturebackend.model.dto.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;

@Service
@Slf4j
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {

        // 1. 校验图片
        validatePicture(multipartFile);

        // 2. 构造图片上传信息和地址
        // a. 获取所需要到的文件信息
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        // b. 构造上传地址
        String uploadPath = String.format("%s_%s.%s", new Date(), uuid, FileUtil.getSuffix(originalFilename));

        File file = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);

            // 4. 上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 5. 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            return uploadPictureResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (file != null){
                boolean delete = file.delete();
                if (!delete){
                    log.error("file delete error, filepath = {}", file.getAbsolutePath());
                }
            }
        }

    }

    /**
     * 校验图片
     * @param multipartFile
     */
    private void validatePicture(MultipartFile multipartFile) {
        if (multipartFile == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片不能为空");
        }
        // a. 检验文件大小
        long size = multipartFile.getSize();
        if (size > 1024 * 1024 * 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片不能大于5M");
        }
        // b. 校验文件后缀
        String filename = multipartFile.getOriginalFilename();
        if (filename == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }
        String suffix = filename.substring(filename.lastIndexOf("."));
        if (!".png".equals(suffix) && !".jpg".equals(suffix) && !".jpeg".equals(suffix)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片格式错误");
        }
    }

}




