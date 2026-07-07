package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liangshou.common.utils.PageResult;
import com.liangshou.infrastructure.datasource.po.SysUserPO;
import com.liangshou.infrastructure.datasource.support.ISysUserSupport;
import com.liangshou.service.ISysUserService;
import com.liangshou.service.dto.SysUserDTO;
import com.liangshou.service.vo.SysUserVO;
import com.liangshou.agentic.application.ITdAgentProfileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements ISysUserService {

    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    private final ISysUserSupport support;
    private final PasswordEncoder passwordEncoder;
    private final ITdAgentProfileService profileService;

    @Override
    public SysUserVO getById(Long id) {
        return toVO(support.getById(id));
    }

    @Override
    public SysUserVO getByUserId(String userId) {
        return toVO(findByUserIdOrThrow(userId));
    }

    @Override
    public PageResult<SysUserVO> page(int current, int size) {
        Page<SysUserPO> page = support.page(new Page<>(current, size));
        List<SysUserVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), records, page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserVO register(SysUserDTO dto) {
        String userId = normalizeRequired(dto.getUserId(), "userId 不能为空");
        if (support.lambdaQuery().eq(SysUserPO::getUserId, userId).exists()) {
            throw new IllegalArgumentException("userId 已存在");
        }

        SysUserPO po = new SysUserPO();
        po.setUserId(userId);
        po.setPassword(passwordEncoder.encode(normalizeRequired(dto.getPassword(), "password 不能为空")));
        po.setNickname(normalizeNickname(dto.getNickname(), userId));
        po.setRole(normalizeRole(dto.getRole()));
        support.save(po);

        // 初始化用户 Profile 配置（不阻塞注册流程）
        try {
            profileService.initializeUser(userId);
            log.info("用户注册成功，Profile 初始化完成 - userId: {}", userId);
        } catch (Exception e) {
            log.error("用户注册成功，但 Profile 初始化失败 - userId: {}, error: {}", userId, e.getMessage(), e);
            // 不抛出异常，Profile 初始化失败不影响注册成功
            // 可以通过定时任务或手动触发补偿初始化
        }

        return toVO(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserVO updateProfile(String currentUserId, SysUserDTO dto) {
        SysUserPO current = findByUserIdOrThrow(currentUserId);
        if (dto.getNickname() != null) {
            current.setNickname(normalizeNickname(dto.getNickname(), current.getUserId()));
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            current.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        support.updateById(current);
        return toVO(current);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SysUserDTO dto) {
        register(dto);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(SysUserDTO dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        SysUserPO current = support.getById(dto.getId());
        if (current == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (dto.getUserId() != null && !dto.getUserId().isBlank() && !dto.getUserId().equals(current.getUserId())) {
            if (support.lambdaQuery().eq(SysUserPO::getUserId, dto.getUserId().trim()).exists()) {
                throw new IllegalArgumentException("userId 已存在");
            }
            current.setUserId(dto.getUserId().trim());
        }
        if (dto.getNickname() != null) {
            current.setNickname(normalizeNickname(dto.getNickname(), current.getUserId()));
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            current.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            current.setRole(normalizeRole(dto.getRole()));
        }
        return support.updateById(current);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return support.removeById(id);
    }

    private SysUserPO findByUserIdOrThrow(String userId) {
        SysUserPO po = support.lambdaQuery()
                .eq(SysUserPO::getUserId, normalizeRequired(userId, "userId 不能为空"))
                .one();
        if (po == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return po;
    }

    private SysUserVO toVO(SysUserPO po) {
        if (po == null) {
            return null;
        }
        SysUserVO vo = new SysUserVO();
        vo.setId(po.getId());
        vo.setUserId(po.getUserId());
        vo.setNickname(normalizeNickname(po.getNickname(), po.getUserId()));
        vo.setRole(normalizeRole(po.getRole()));
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeNickname(String nickname, String fallback) {
        if (nickname == null || nickname.isBlank()) {
            return fallback;
        }
        return nickname.trim();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "USER";
        }
        return role.trim().toUpperCase();
    }
}
