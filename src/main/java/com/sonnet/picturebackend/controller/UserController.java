package com.sonnet.picturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sonnet.picturebackend.annotation.AuthCheck;
import com.sonnet.picturebackend.common.BaseResponse;
import com.sonnet.picturebackend.common.Constant;
import com.sonnet.picturebackend.common.DeleteRequest;
import com.sonnet.picturebackend.common.ResultUtils;
import com.sonnet.picturebackend.exception.BusinessException;
import com.sonnet.picturebackend.exception.ErrorCode;
import com.sonnet.picturebackend.exception.ThrowUtils;
import com.sonnet.picturebackend.model.dto.user.*;
import com.sonnet.picturebackend.model.entry.User;
import com.sonnet.picturebackend.model.vo.UserVO;
import com.sonnet.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest userRegisterRequest) {

        // 1. 基本校验
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);

        String userAccount = userRegisterRequest.getUserAccount();
        String password = userRegisterRequest.getPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        // 2. 创建用户
        long restlt = userService.userRegister(userAccount, password, checkPassword);

        // 3. 返回结果
        return ResultUtils.success(restlt);
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return 脱敏登录用户信息
     */
    @PostMapping("/login")
    public BaseResponse<UserVO> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {

        // 1. 校验基本参数
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 登录
        String userAccount = userLoginRequest.getUserAccount();
        String password = userLoginRequest.getUserPassword();
        UserVO loginUser = userService.userLogin(userAccount, password, request);

        // 3. 返回结果
        return ResultUtils.success(loginUser);

    }

    /**
     * 获取当前登录用户信息
     * @param request
     * @return 脱敏登录用户信息
     */
    @GetMapping("/getLoginUser")
    public BaseResponse<UserVO> getLoginUserVO(HttpServletRequest request){
        // 1. 基本校验

        // 2. 获取登录用户
        UserVO loginUserVO = userService.getLoginUserVO(request);

        // 3. 返回结果
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 退出登录
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request){

        // 1. 基本校验
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        // 2. 退出登录
        boolean logout = userService.logout(request);

        // 3. 返回结果
        return ResultUtils.success(logout);

    }

    /**
     * 添加用户
     * @param userAddRequest
     * @return 新增用户id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        // 1. 校验参数
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 添加用户
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        if (user.getUserPassword() == null){
            user.setUserPassword(userService.getEncryptPassword("12345678"));
        }
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 3. 返回结果
        return ResultUtils.success(user.getId());
    }

    /**
     * 获取用户信息
     * @param id
     * @return 用户脱敏信息
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id) {
        // 1. 校验参数
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);

        // 2. 获取用户
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        // 3. 获取脱敏用户
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        // 4. 返回结果
        return ResultUtils.success(userVO);
    }

    /**
     * 根据 id 查询用户信息
     * @param id
     * @return 返回对应用户信息
     */
    @GetMapping("/get")
    public BaseResponse<User> getUserById(Long id) {
        // 1. 校验参数
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);

        // 2. 获取用户
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        // 3. 返回结果
        return ResultUtils.success(user);
    }

    /**
     * 删除用户
     * @param deleteRequest
     * @return 删除指定用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        // 1. 基本参数校验
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 2. 删除用户
        boolean result = userService.removeById(deleteRequest.getId());

        // 3. 返回结果
        return ResultUtils.success(result);
    }

    /**
     * 更新用户
     * @param userUpdateRequest
     * @return 更新用户信息
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest){
        // 1. 基本参数校验
        if (userUpdateRequest == null || userUpdateRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 更新用户
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 3. 返回结果
        return ResultUtils.success(result);
    }

    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = Constant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {

        log.info("listUserVOByPage");
        log.info("listUserVOByPage");
        log.info("listUserVOByPage");
        // 1. 基本参数校验
        if (userQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 分页查询
        // a. 获取分页参数
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        if (current <= 0 || pageSize <= 0){
            current = 1L;
            pageSize = 10L;
        }
        // b. 封装查询条件，进行查询
        Page<User> userPage = userService.page(
                new Page<>(current, pageSize), // 分页查询条件
                userService.getQueryWrapper(userQueryRequest)); // 查询符合条件的用户
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);

        // 3. 返回结果
        return ResultUtils.success(userVOPage);
    }

}
