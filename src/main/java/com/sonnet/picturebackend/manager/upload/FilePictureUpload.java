package com.sonnet.picturebackend.manager.upload;

import com.sonnet.picturebackend.exception.BusinessException;
import com.sonnet.picturebackend.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 我再一次感受到了，多态的威力，Java 语言的魅力
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate{

    @Override
    protected void validatePicture(Object inputResource) {

        MultipartFile multipartFile = (MultipartFile) inputResource;

        if (multipartFile == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片不能为空");
        }
        // 检验文件大小
        long size = multipartFile.getSize();
        if (size > 1024 * 1024 * 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片不能大于5M");
        }
        // 校验文件后缀
        String filename = multipartFile.getOriginalFilename();
        if (filename == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }
        String suffix = filename.substring(filename.lastIndexOf("."));
        if (!".png".equals(suffix) && !".jpg".equals(suffix) && !".jpeg".equals(suffix)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片格式错误");
        }

    }

    @Override
    protected String getOriginFileName(Object inputResource) {
        MultipartFile multipartFile = (MultipartFile) inputResource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputResource, File file) {
        MultipartFile multipartFile = (MultipartFile) inputResource;
        try {
            multipartFile.transferTo(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
