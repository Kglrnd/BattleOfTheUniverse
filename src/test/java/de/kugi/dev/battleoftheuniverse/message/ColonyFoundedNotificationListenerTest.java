package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.fleet.ColonyFounded;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColonyFoundedNotificationListenerTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;

    private ColonyFoundedNotificationListener listener;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        MessageService messageService = new MessageService(messageRepository, userRepository, messageSource);
        listener = new ColonyFoundedNotificationListener(messageService);
    }

    @Test
    void notifiesTheOwnerWithResearchAndProductionEfficiencyForEveryProducingBuilding() {
        User owner = new User("alice", "alice@example.com", "hash");
        owner.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        listener.on(new ColonyFounded(1L, 30L, "Colony A", 97.5,
                Map.of("metal_mine", 92.1, "crystal_mine", 105.5)));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message message = captor.getValue();
        assertThat(message.getRecipientUserId()).isEqualTo(1L);
        assertThat(message.getBody())
                .contains("Colony A")
                .contains("97.50%")
                .contains("metal_mine: 92.10%")
                .contains("crystal_mine: 105.50%");
    }
}
