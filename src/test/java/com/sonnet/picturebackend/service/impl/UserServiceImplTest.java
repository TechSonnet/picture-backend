package com.sonnet.picturebackend.service.impl;

import com.sonnet.picturebackend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceImplTest {

    @Resource
    private UserService userService;

    @Test
    void userRegister() {

        String userAccount = "testUser";
        String password = "password123";
        String checkPassword = "password123";

        long userId = userService.userRegister(userAccount, password, checkPassword);
        System.out.println(userId);
    }


}