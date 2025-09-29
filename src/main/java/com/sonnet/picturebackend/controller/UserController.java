package com.sonnet.picturebackend.controller;

import com.sonnet.picturebackend.common.BaseResponse;
import com.sonnet.picturebackend.common.ResultUtils;
import com.sonnet.picturebackend.exception.ErrorCode;
import com.sonnet.picturebackend.exception.ThrowUtils;
import com.sonnet.picturebackend.model.dto.UserRegisterRequest;
import com.sonnet.picturebackend.model.dto.UserLoginRequest;
import com.sonnet.picturebackend.model.vo.UserVO;
import com.sonnet.picturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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

}
