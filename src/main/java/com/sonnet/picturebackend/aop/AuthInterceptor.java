package com.sonnet.picturebackend.aop;

import com.sonnet.picturebackend.annotation.AuthCheck;
import com.sonnet.picturebackend.exception.BusinessException;
import com.sonnet.picturebackend.exception.ErrorCode;
import com.sonnet.picturebackend.model.enums.UserRoleEnum;
import com.sonnet.picturebackend.model.vo.UserVO;
import com.sonnet.picturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 1. 获取注解信息
        String mustRole = authCheck.mustRole();

        // 2. 获取当前登录用户和用户所具有的权限
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        UserVO loginUser = userService.getLoginUserVO(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

        // 3. 不需要权限，直接放行
        if (mustRoleEnum == null){
            return joinPoint.proceed();
        }

        // 4. 需要权限，根据具体情况进行处理
        // a.获取当前登录用户的权限
        String userRole = userService.getLoginUserVO(request).getUserRole();
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(userRole);
        // b.判断当前用户的权限
        // 当前用户必须具有权限
        if (userRoleEnum == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "user have to a role");
        }
        // 当前用户必须有管理员权限
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum)  // 检查此功能是否需要管理员权限
                && !UserRoleEnum.ADMIN.equals(userRoleEnum)){ // 检查当前用户是否是管理员
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}
