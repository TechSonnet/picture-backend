package com.sonnet.picturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.sonnet.picturebackend.config.CosClientConfig;
import com.sonnet.picturebackend.manager.CosManager;
import com.sonnet.picturebackend.model.vo.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * 这里捋一下，为什么使用了模板模式
 * 在 FileManager 中，有两个十分相似的方法， uploadPicture() 和 uploadPictureByUrl ()
 * 这两个方法逻辑十分相似，有许多公共可服用的逻辑，因此非常适合使用模板模式
 * 为了使用模板模式，可以在模板模式中抽取出公共流程，而异构流程，就可以放在子类中实现
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     *  模板方法，定义流程骨架和通用实现
     */
    public UploadPictureResult uploadPicture(Object inputResource, String uploadPathPrefix) {

        // 参数校验
        /// 第一处异构
        validatePicture(inputResource);

        // 构造图片上传路径
        String uuid = RandomUtil.randomString(16);
        /// 第二处异构
        String originFileName = getOriginFileName(inputResource);
        String uploadFileName = StrUtil.format("{}_{}.{}",
                DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originFileName));
        /// 这个构造方式，要好好思索下
        String uploadPath = StrUtil.format("{}/{}", uploadPathPrefix, uploadFileName);

        // 创建临时文件
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
            /// 第三处异构
            processFile(inputResource, file);

            // 上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            return getUploadPictureResult(originFileName, file, imageInfo, uploadPath);

        } catch (IOException e) {
            log.error("upload picture error, inputResource = {}, uploadPathPrefix = {}", inputResource, uploadPathPrefix, e);
            throw new RuntimeException(e);
        } finally {
            /// 第四处异构
            deleteTempFile(file);
        }

    }



    /**
     *  抽象方法，验证图片信息
     */
    protected abstract void validatePicture(Object inputResource);

    /**
     *  抽象方法，获取图片原始文件名
     */
    protected abstract String getOriginFileName(Object inputResource);

    /**
     *  抽象方法，处理图片
     */
    protected abstract void processFile(Object inputResource, File file);

    /**
     *  抽象方法，封装图片信息
     */
    protected UploadPictureResult getUploadPictureResult(String originFileName, File file, ImageInfo imageInfo, String uploadPath) {
        // 封装图片信息 uploadPictureResult
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setPicName(FileUtil.mainName(originFileName));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(imageInfo.getWidth());
        uploadPictureResult.setPicHeight(imageInfo.getHeight());
        uploadPictureResult.setPicScale(NumberUtil.round(imageInfo.getWidth() * 1.0 / imageInfo.getHeight(), 2).doubleValue());
        uploadPictureResult.setPicFormat(FileUtil.extName(originFileName));
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        return uploadPictureResult;
    }

    /**
     *  抽象方法，删除临时文件
     */
    protected void deleteTempFile(File file){
        if (file != null) {
            boolean del = FileUtil.del(file);
            if (!del) {
                log.error("delete temp file fail, file = {}", file);
            }
        }
    }
}
