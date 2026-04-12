package com.liangshou.common.security;

import com.liangshou.infrastructure.datasource.po.SysUserPO;
import com.liangshou.infrastructure.datasource.support.ISysUserSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户详情服务实现类。
 * <p>用于从数据库中加载用户信息，供 Spring Security 进行认证和授权。</p>
 * <p>实现了 {@link UserDetailsService} 接口，提供根据用户名加载用户详情的功能。</p>
 * <p>当前实现使用硬编码的用户数据进行演示，实际项目中应注入 UserRepository 或 UserService 来查询数据库。</p>
 *
 * @author liangshou
 * @version 1.0
 * @see UserDetailsService
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class UserDetailsServiceImpl implements UserDetailsService {

    private final ISysUserSupport sysUserSupport;

    /**
     * 根据用户名加载用户详情。
     * <p>从数据源（当前为硬编码数据）中查询用户信息，并返回包含用户凭证和权限的 UserDetails 对象。</p>
     * <p>如果找到名为 "admin" 的用户，则返回具有 ROLE_ADMIN 角色的用户详情；否则抛出异常。</p>
     *
     * @param username 要查询的用户名
     * @return 包含用户详情的对象
     * @throws UsernameNotFoundException 如果未找到指定用户名的用户
     * @see UserDetails
     * @see org.springframework.security.core.userdetails.User
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUserPO user = sysUserSupport.lambdaQuery()
                .eq(SysUserPO::getUserId, username)
                .one();
        if (user == null) {
            throw new UsernameNotFoundException("User Not Found with userId: " + username);
        }
        String role = user.getRole() == null || user.getRole().isBlank() ? "USER" : user.getRole().trim();
        return new User(
                user.getUserId(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
        );
    }
}
