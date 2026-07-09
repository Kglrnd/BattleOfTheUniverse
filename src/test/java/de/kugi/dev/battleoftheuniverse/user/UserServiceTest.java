package de.kugi.dev.battleoftheuniverse.user;

import de.kugi.dev.battleoftheuniverse.user.dto.AdminUserView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
