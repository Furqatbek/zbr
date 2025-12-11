package com.fooddelivery.auth.security;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UserDetails implementation for Spring Security.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final Set<Role> roles;
    private final UserStatus status;
    private final boolean accountLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.roles = user.getRoles();
        this.status = user.getStatus();
        this.accountLocked = user.isAccountLocked();
        this.authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
    }

    public static UserPrincipal create(User user) {
        return new UserPrincipal(user);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return status != UserStatus.DELETED;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked && status != UserStatus.SUSPENDED && status != UserStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return email;
        }
        return String.format("%s %s",
                firstName != null ? firstName : "",
                lastName != null ? lastName : "").trim();
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(Role... rolesToCheck) {
        for (Role role : rolesToCheck) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
