package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.combat.PlanetDestroyed;
import de.kugi.dev.battleoftheuniverse.combat.PlanetInvaded;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanetEffectNotificationListenerTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;

    private PlanetEffectNotificationListener listener;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        MessageService messageService = new MessageService(messageRepository, userRepository, messageSource);
        listener = new PlanetEffectNotificationListener(messageService, userRepository);
    }

    @Test
    void planetDestroyedNotifiesBothTheAttackerAndTheDefender() {
        User attacker = new User("alice", "alice@example.com", "hash");
        attacker.setId(1L);
        User defender = new User("bob", "bob@example.com", "hash");
        defender.setId(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(attacker));
        when(userRepository.findById(2L)).thenReturn(Optional.of(defender));

        listener.on(new PlanetDestroyed(1L, 2L, 20L, "Bob's Colony", "[1:2:3]", Instant.now()));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Message::getRecipientUserId).containsExactlyInAnyOrder(1L, 2L);
        assertThat(captor.getAllValues()).extracting(Message::getBody).anySatisfy(body -> assertThat(body).contains("[1:2:3]"));
    }

    @Test
    void planetInvadedNotifiesBothTheAttackerAndTheDefender() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        listener.on(new PlanetInvaded(1L, 2L, 20L, "Bob's Colony", Instant.now()));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Message::getRecipientUserId).containsExactlyInAnyOrder(1L, 2L);
    }
}
