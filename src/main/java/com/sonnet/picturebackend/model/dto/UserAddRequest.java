package com.sonnet.picturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String userAccount;
    /**
     * 密码
     */
    private String password;
    /**
     * 确认密码
     */
    private String checkPassword;

}
