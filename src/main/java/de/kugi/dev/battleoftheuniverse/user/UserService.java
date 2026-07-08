package de.kugi.dev.battleoftheuniverse.user;

import de.kugi.dev.battleoftheuniverse.user.dto.AdminUserView;
import de.kugi.dev.battleoftheuniverse.user.dto.RegisterRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    /** Convenience for callers outside this module, which can't see the {@code dto} package. */
    @Transactional
    public UserView register(String username, String email, String password) {
        return register(new RegisterRequest(username, email, password));
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
        user = userRepository.save(user);

        events.publishEvent(new UserRegistered(user.getId(), user.getUsername()));

        return toView(user);
    }

    public UserView toView(User user) {
        return new UserView(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public List<AdminUserView> listAll() {
        return userRepository.findAll().stream().map(AdminUserView::from).toList();
    }

    @Transactional
    public AdminUserView changeRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(role);
        userRepository.save(user);
        return AdminUserView.from(user);
    }
}
