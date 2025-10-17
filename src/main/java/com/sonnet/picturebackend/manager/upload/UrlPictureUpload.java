package com.sonnet.picturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.sonnet.picturebackend.exception.BusinessException;
import com.sonnet.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

@Service
@Slf4j
public class UrlPictureUpload extends PictureUploadTemplate{

    /**
     * 通过 url 校验图片
     * @param inputResource
     */
    @Override
    protected void validatePicture(Object inputResource) {

        String fileUrl = (String) inputResource;

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

    /**
     * 获取图片原始文件名
     * @param inputResource
     * @return
     */
    @Override
    protected String getOriginFileName(Object inputResource) {
        String fileUrl = (String) inputResource;
        String suffix = FileUtil.getSuffix(fileUrl);
        return FileUtil.mainName(fileUrl) + "." + suffix;
    }

    /**
     * 下载图片
     * @param inputResource
     * @param file
     */
    @Override
    protected void processFile(Object inputResource, File file) {
        String fileUrl = (String) inputResource;
        HttpUtil.downloadFile(fileUrl, file);
    }

}
