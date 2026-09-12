package com.nursery.config;

import com.nursery.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 登录主体，从 JWT 还原。
 * 访问器显式实现（不依赖 Lombok），保证 UserDetails 契约在任何编译配置下都成立。
 */
public class AuthUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String name;
    private final User.Role role;

    public AuthUser(Long id, String username, String name, User.Role role) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.role = role;
    }

    public static AuthUser from(User u) {
        return new AuthUser(u.getId(), u.getUsername(), u.getName(), u.getRole());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User.Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
