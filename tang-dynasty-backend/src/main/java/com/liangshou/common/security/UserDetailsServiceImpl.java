package com.liangshou.common.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    // For demonstration, we just use a hardcoded user or would query from DB.
    // In actual implementation, inject UserRepository/Service to load user.

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Here we mock the database fetch
        if ("admin".equals(username)) {
            return new User(
                    "admin",
                    "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi", // "admin123"
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
        }
        throw new UsernameNotFoundException("User Not Found with username: " + username);
    }
}
