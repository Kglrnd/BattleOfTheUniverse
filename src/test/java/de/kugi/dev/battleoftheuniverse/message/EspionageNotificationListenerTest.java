package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.fleet.EspionageResolved;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipQuantity;
import de.kugi.dev.battleoftheuniverse.resource.dto.ResourceView;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspionageNotificationListenerTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;

    private EspionageNotificationListener listener;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        MessageService messageService = new MessageService(messageRepository, userRepository, messageSource);
        listener = new EspionageNotificationListener(messageService, userRepository);
    }

    @Test
    void successfulEspionageSendsOnlyTheAttackerAReportWithFleetAndResources() {
        User attacker = new User("alice", "alice@example.com", "hash");
        attacker.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(attacker));

        EspionageResolved event = new EspionageResolved(1L, 2L, 20L, "Bob's Planet", true,
                List.of(new ShipQuantity("light_fighter", 3)),
                List.of(new ResourceView(ResourceKey.METAL, "Metal", 500)));

        listener.on(event);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, times(1)).save(captor.capture());
        Message report = captor.getValue();
        assertThat(report.getRecipientUserId()).isEqualTo(1L);
        assertThat(report.getBody()).contains("light_fighter: 3").contains("500");
    }

    @Test
    void failedEspionageNotifiesTheAttackerOfFailureAndTheDefenderOfDetection() {
        User attacker = new User("alice", "alice@example.com", "hash");
        attacker.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(attacker));

        EspionageResolved event = new EspionageResolved(1L, 2L, 20L, "Bob's Planet", false, List.of(), List.of());

        listener.on(event);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Message::getRecipientUserId).containsExactlyInAnyOrder(1L, 2L);
    }
}
