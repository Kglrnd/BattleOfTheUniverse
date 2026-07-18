package de.kugi.dev.battleoftheuniverse.user;

import de.kugi.dev.battleoftheuniverse.user.dto.AdminUserView;
import de.kugi.dev.battleoftheuniverse.user.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final UserMapper userMapper;

    /** Convenience for callers outside this module, which can't see the {@code dto} package. */
    @Transactional
    public UserView register(String username, String email, String password) {
        return register(new RegisterRequest(username, email, password, null));
    }

    @Transactional
    public UserView register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User(request.username(), request.email(), passwordEncoder.encode(request.password()));
        if (request.preferredLanguage() != null) {
            user.setPreferredLanguage(request.preferredLanguage());
        }
        user = userRepository.save(user);

        events.publishEvent(new UserRegistered(user.getId(), user.getUsername()));

        return userMapper.toView(user);
    }

    @Transactional
    public UserView updatePreferredLanguage(Long userId, String language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPreferredLanguage(language);
        userRepository.save(user);
        return userMapper.toView(user);
    }

    public List<AdminUserView> listAll() {
        return userRepository.findAll().stream().map(userMapper::toAdminView).toList();
    }

    @Transactional
    public AdminUserView changeRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(role);
        userRepository.save(user);
        return userMapper.toAdminView(user);
    }

    /** Deactivating flips {@link User#isActive()} to false, which Spring Security's {@code isEnabled()} then rejects at login - an active session isn't forcibly killed, but the account can't log in again. */
    @Transactional
    public AdminUserView setActive(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setActive(active);
        userRepository.save(user);
        return userMapper.toAdminView(user);
    }
}
