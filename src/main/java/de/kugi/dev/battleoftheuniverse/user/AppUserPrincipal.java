package de.kugi.dev.battleoftheuniverse.user;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.List;

/** Adapts a {@link User} to Spring Security while still exposing the entity's id/role. */
@RequiredArgsConstructor
public class AppUserPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final User user;

    public @Nullable Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public Role getRole() {
        return user.getRole();
    }

    public String getPreferredLanguage() {
        return user.getPreferredLanguage();
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
