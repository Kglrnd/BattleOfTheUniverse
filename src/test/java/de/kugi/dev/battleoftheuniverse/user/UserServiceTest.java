package de.kugi.dev.battleoftheuniverse.user;

import de.kugi.dev.battleoftheuniverse.user.dto.AdminUserView;
import de.kugi.dev.battleoftheuniverse.user.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher events;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder, events, new UserMapperImpl());
    }

    @Test
    void listAllMapsEveryUserToAnAdminView() {
        User alice = new User("alice", "alice@example.com", "hash");
        alice.setId(1L);
        User bob = new User("bob", "bob@example.com", "hash");
        bob.setId(2L);
        bob.setRole(Role.MODERATOR);
        when(userRepository.findAll()).thenReturn(List.of(alice, bob));

        List<AdminUserView> views = service.listAll();

        assertThat(views).hasSize(2);
        assertThat(views).extracting(AdminUserView::username).containsExactly("alice", "bob");
        assertThat(views).filteredOn(v -> v.username().equals("bob")).first()
                .satisfies(v -> assertThat(v.role()).isEqualTo(Role.MODERATOR));
    }

    @Test
    void changeRoleUpdatesAndPersistsTheUsersRole() {
        User user = new User("alice", "alice@example.com", "hash");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AdminUserView view = service.changeRole(1L, Role.MODERATOR);

        assertThat(view.role()).isEqualTo(Role.MODERATOR);
        assertThat(user.getRole()).isEqualTo(Role.MODERATOR);
    }

    @Test
    void changeRoleRejectsAnUnknownUserId() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(99L, Role.ADMIN))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void setActiveUpdatesAndPersistsTheUsersActiveFlag() {
        User user = new User("alice", "alice@example.com", "hash");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AdminUserView view = service.setActive(1L, false);

        assertThat(view.active()).isFalse();
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void setActiveRejectsAnUnknownUserId() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setActive(99L, false))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updatePreferredLanguageUpdatesAndPersistsTheUsersLanguage() {
        User user = new User("alice", "alice@example.com", "hash");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserView view = service.updatePreferredLanguage(1L, "de");

        assertThat(view.preferredLanguage()).isEqualTo("de");
        assertThat(user.getPreferredLanguage()).isEqualTo("de");
    }

    @Test
    void updatePreferredLanguageRejectsAnUnknownUserId() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePreferredLanguage(99L, "de"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void registerCreatesAUserPublishesAnEventAndReturnsAView() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });

        UserView view = service.register(new RegisterRequest("alice", "alice@example.com", "secret123", "de"));

        assertThat(view.id()).isEqualTo(7L);
        assertThat(view.username()).isEqualTo("alice");
        var eventCaptor = ArgumentCaptor.forClass(UserRegistered.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(7L);
    }

    @Test
    void registerThreeArgConvenienceOverloadDelegatesWithNoPreferredLanguage() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserView view = service.register("bob", "bob@example.com", "secret123");

        assertThat(view.username()).isEqualTo("bob");
    }

    @Test
    void registerRejectsAnAlreadyTakenUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("alice", "new@example.com", "secret123", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Username already taken");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsAnAlreadyRegisteredEmail() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("alice", "alice@example.com", "secret123", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email already registered");
        verify(userRepository, never()).save(any());
    }
}
