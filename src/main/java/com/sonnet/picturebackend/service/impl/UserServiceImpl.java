package com.sonnet.picturebackend.service.impl;

import ch.qos.logback.classic.spi.EventArgUtil;
import cn.hutool.Hutool;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sonnet.picturebackend.common.Constant;
import com.sonnet.picturebackend.exception.BusinessException;
import com.sonnet.picturebackend.exception.ErrorCode;
import com.sonnet.picturebackend.exception.ThrowUtils;
import com.sonnet.picturebackend.model.dto.UserQueryRequest;
import com.sonnet.picturebackend.model.entry.User;
import com.sonnet.picturebackend.mapper.UserMapper;
import com.sonnet.picturebackend.model.enums.UserRoleEnum;
import com.sonnet.picturebackend.model.vo.UserVO;
import com.sonnet.picturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-09-28 10:38:01
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {


    /**
     * 用户注册
     * @param userAccount
     * @param password
     * @param checkPassword
     * @return userId
     */
    @Override
    public long userRegister(String userAccount, String password, String checkPassword) {
        // 1. 详细校验
        // 参数校验
        if (userAccount == null || password == null || checkPassword == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "params is null");
        }

        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "username is too short");
        }

        if (password.length() < 6 || checkPassword.length() < 6){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "password is too short");
        }

        if (!password.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "password is not equal");
        }
        // 账户不可重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "userAccount is exist");
        }

        // 2. 插入数据
        // 信息加密
        String digestPassword = getEncryptPassword(password);
        // 插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserName("luck dog");
        user.setUserPassword(digestPassword);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = this.save(user);
        if (!save){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "save user error");
        }

        // 3. 返回结果
        return user.getId();
    }

    @Override
    public UserVO userLogin(String userAccount, String password, HttpServletRequest request) {

        // 1. 详细校验
        if (userAccount == null || password == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "params is null");
        }
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "username is too short");
        }
        if (password.length() < 6){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "password is too short");
        }


        // 2. 登录
        // 判断是否已经登录
        if (request.getSession().getAttribute(Constant.USER_LOGIN_STATE) != null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "user is login");
        }
        // 查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", getEncryptPassword(password));
        User user = this.getOne(queryWrapper);
        Optional.ofNullable(user).orElseThrow(
                () -> new BusinessException(ErrorCode.PARAMS_ERROR, "userAccount or password is error")
        );
        // 存储用户态
        request.getSession().setAttribute(Constant.USER_LOGIN_STATE, user);


        // 3. 返回登录信息
        return getUserVO(user);
    }

    /**
     * 获取登录用户信息
     * @param request
     * @return 登录用户信息
     */
    @Override
    public UserVO getLoginUserVO(HttpServletRequest request) {
        // 1. 详细校验参数

        // 2. 登录状态判断
        if (request.getSession().getAttribute(Constant.USER_LOGIN_STATE) == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 3. 获取登录状态
        User user = (User) request.getSession().getAttribute(Constant.USER_LOGIN_STATE);
        return getUserVO(user);
    }

    /**
     * 退出登录
     * @param request
     * @return 退出登录结果
     */
    @Override
    public boolean logout(HttpServletRequest request) {
        // 1. 详细校验参数
        // 2. 销毁登录态
        // 判断是否已登录
        if (request.getSession().getAttribute(Constant.USER_LOGIN_STATE) == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "user is not login");
        }
        // 移除登录态
        request.getSession().removeAttribute(Constant.USER_LOGIN_STATE);

        return true;
    }


    /**
     * 获取用户脱敏信息
     * @param user
     * @return 脱敏信息
     */
    public UserVO getUserVO(User user) {
        if (user == null){
            return null;
        }
        UserVO loginUserVO = new UserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取用户列表脱敏信息
     * @param userList
     * @return 脱敏信息列表
     */
    public List<UserVO> getUserVOList(List<User> userList) {
        // 1. 基本校验
        if(userList == null){
            return null;
        }

        // 2. 转换并返回结果
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 获取加密密码
     * @param password
     * @return 加密后的密码
     */
    public String getEncryptPassword(String password) {
        return DigestUtil.sha256Hex(Constant.SALT +  password);
    }

    @Override
    public Wrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "params is null");
        }

        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.eq(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.eq(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }


    /**
     * 获取登录用户
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {

        if (request.getSession().getAttribute(Constant.USER_LOGIN_STATE) == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return (User) request.getSession().getAttribute(Constant.USER_LOGIN_STATE);
    }
}




