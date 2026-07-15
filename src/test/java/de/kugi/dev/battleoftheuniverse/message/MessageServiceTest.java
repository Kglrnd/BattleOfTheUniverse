package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;

    private MessageService service;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        // Matches application.yml's spring.messages.fallback-to-system-locale: false -
        // otherwise this test's assertions would depend on the JVM's default locale.
        messageSource.setFallbackToSystemLocale(false);
        service = new MessageService(messageRepository, userRepository, messageSource);
    }

    @Test
    void rendersTheSubjectAndBodyInTheRecipientsPreferredLanguage() {
        User recipient = new User("alice", "alice@example.com", "hash");
        recipient.setId(1L);
        recipient.setPreferredLanguage("de");
        when(userRepository.findById(1L)).thenReturn(Optional.of(recipient));

        service.sendSystemMessage(1L, "message.colonyFounded.subject", new Object[0],
                "message.colonyFounded.body", new Object[] { "Kolonie A", "99,50%" });

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message saved = captor.getValue();
        assertThat(saved.getSubject()).isEqualTo("Neue Kolonie gegründet");
        assertThat(saved.getBody()).contains("Kolonie A").contains("Forschungseffizienz");
    }

    @Test
    void defaultsToEnglishWhenTheRecipientHasNoStoredPreference() {
        User recipient = new User("bob", "bob@example.com", "hash");
        recipient.setId(2L);
        recipient.setPreferredLanguage("en");
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        service.sendSystemMessage(2L, "message.colonyFounded.subject", new Object[0],
                "message.colonyFounded.body", new Object[] { "Colony A", "99.50%" });

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("New colony founded");
    }

    @Test
    void defaultsToEnglishWhenTheRecipientIsUnknown() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        service.sendSystemMessage(99L, "message.colonyFounded.subject", new Object[0],
                "message.colonyFounded.body", new Object[] { "Colony A", "99.50%" });

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("New colony founded");
    }
}
