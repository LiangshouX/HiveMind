package com.liangshou.adapter.controller;

import com.liangshou.common.utils.JwtUtils;
import com.liangshou.agentic.common.utils.Result;
import com.liangshou.service.ISysUserService;
import com.liangshou.service.dto.SysUserDTO;
import com.liangshou.service.vo.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证接口", description = "登录授权与JWT分发")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final ISysUserService sysUserService;
    private final JwtUtils jwtUtils;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<SysUserVO> register(@Valid @RequestBody RegisterRequest request) {
        SysUserDTO dto = new SysUserDTO();
        dto.setUserId(request.getUserId().trim());
        dto.setPassword(request.getPassword());
        dto.setNickname(request.getNickname());
        return Result.success(sysUserService.register(dto));
    }

    @Operation(summary = "用户登录", description = "使用 userId 和密码登录并获取 JWT Token")
    @PostMapping({"", "/login"})
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserId().trim(), request.getPassword()));
        SysUserVO user = sysUserService.getByUserId(authentication.getName());
        String token = jwtUtils.generateToken(user.getUserId());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresAt(Instant.now().plusMillis(jwtUtils.getExpirationTime()).toString());
        response.setUser(user);
        return Result.success(response);
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public Result<SysUserVO> me(Authentication authentication) {
        return Result.success(sysUserService.getByUserId(authentication.getName()));
    }

    @Operation(summary = "更新当前登录用户信息")
    @PutMapping("/me")
    public Result<SysUserVO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        SysUserDTO dto = new SysUserDTO();
        dto.setNickname(request.getNickname());
        dto.setPassword(request.getPassword());
        return Result.success(sysUserService.updateProfile(authentication.getName(), dto));
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "userId 不能为空")
        private String userId;

        @NotBlank(message = "password 不能为空")
        private String password;

        private String nickname;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "userId 不能为空")
        private String userId;

        @NotBlank(message = "password 不能为空")
        private String password;
    }

    @Data
    public static class ProfileUpdateRequest {
        private String nickname;
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private String expiresAt;
        private SysUserVO user;
    }
}
