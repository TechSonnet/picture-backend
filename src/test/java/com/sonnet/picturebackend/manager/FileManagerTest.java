package com.sonnet.picturebackend.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import javax.annotation.Resource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FileManagerTest {

    @Resource
    private FileManager fileManager;

    @Test
    void testUploadToCOS() throws Exception {
        // 1. 构造多媒体文件
        FileInputStream fileInputStream = new FileInputStream(
                "D:\\Projects\\IdeaProjects\\picture-backend\\src\\main\\java\\com\\sonnet\\picturebackend\\doc\\sql_record.sql");
        MockMultipartFile testFile = new MockMultipartFile(
                "test_file",
                "sql_record.sql",
                "application/octet-stream",
                fileInputStream
        );

        // 2. 构造各种参数
        String bucketName = "chang-1303694861";
        String key = "test/" + "test.sql";

        // 3. 上传到 COS
        String uploadToCOS = fileManager.uploadToCOS(testFile, bucketName, key);
    }
}