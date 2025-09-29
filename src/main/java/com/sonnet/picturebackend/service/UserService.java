package com.sonnet.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.sonnet.picturebackend.model.dto.UserQueryRequest;
import com.sonnet.picturebackend.model.entry.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sonnet.picturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Administrator
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-09-28 10:38:01
*/
public interface UserService extends IService<User> {

    long userRegister(String username, String password, String checkPassword);

    UserVO userLogin(String userAccount, String password, HttpServletRequest request);

    UserVO getLoginUserVO(HttpServletRequest request);

    boolean logout(HttpServletRequest request);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    String getEncryptPassword(String password);

    Wrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
