package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.message.dto.SendMessageRequest;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
                "message.colonyFounded.intro", new Object[] { "Kolonie A" });

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message saved = captor.getValue();
        assertThat(saved.getSubject()).isEqualTo("Neue Kolonie gegründet");
        assertThat(saved.getBody()).contains("Kolonie A");
    }

    @Test
    void defaultsToEnglishWhenTheRecipientHasNoStoredPreference() {
        User recipient = new User("bob", "bob@example.com", "hash");
        recipient.setId(2L);
        recipient.setPreferredLanguage("en");
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        service.sendSystemMessage(2L, "message.colonyFounded.subject", new Object[0],
                "message.colonyFounded.intro", new Object[] { "Colony A" });

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("New colony founded");
    }

    @Test
    void defaultsToEnglishWhenTheRecipientIsUnknown() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        service.sendSystemMessage(99L, "message.colonyFounded.subject", new Object[0],
                "message.colonyFounded.intro", new Object[] { "Colony A" });

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("New colony founded");
    }

    @Test
    void sendSavesAPlayerMessageAndReturnsAView() {
        User recipient = new User("bob", "bob@example.com", "hash");
        recipient.setId(2L);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(recipient));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(42L);
            return m;
        });

        var view = service.send(1L, "alice", new SendMessageRequest("bob", "Hi", "Hello there"));

        assertThat(view.id()).isEqualTo(42L);
        assertThat(view.senderUsername()).isEqualTo("alice");
        assertThat(view.recipientUsername()).isEqualTo("bob");
        assertThat(view.subject()).isEqualTo("Hi");
        assertThat(view.body()).isEqualTo("Hello there");
    }

    @Test
    void sendRejectsAnUnknownRecipient() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send(1L, "alice", new SendMessageRequest("ghost", "Hi", "Hello")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Recipient not found");
        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendRejectsMessagingYourself() {
        User self = new User("alice", "alice@example.com", "hash");
        self.setId(1L);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> service.send(1L, "alice", new SendMessageRequest("alice", "Hi", "Hello")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot send a message to yourself");
        verify(messageRepository, never()).save(any());
    }

    @Test
    void inboxResolvesSenderUsernamesAndFallsBackToSystemForNullSenders() {
        Message fromSystem = new Message(null, 1L, "Sys", "body", MessageType.SYSTEM, Instant.now());
        Message fromPlayer = new Message(2L, 1L, "Hi", "body", MessageType.PLAYER, Instant.now());
        when(messageRepository.findByRecipientUserIdOrderBySentAtDesc(1L)).thenReturn(List.of(fromSystem, fromPlayer));
        User sender = new User("bob", "bob@example.com", "hash");
        sender.setId(2L);
        when(userRepository.findAllById(any())).thenReturn(List.of(sender));

        var views = service.inbox(1L, "alice");

        assertThat(views).hasSize(2);
        assertThat(views).filteredOn(v -> v.subject().equals("Sys")).first()
                .satisfies(v -> assertThat(v.senderUsername()).isEqualTo("System"));
        assertThat(views).filteredOn(v -> v.subject().equals("Hi")).first()
                .satisfies(v -> assertThat(v.senderUsername()).isEqualTo("bob"));
    }

    @Test
    void sentResolvesRecipientUsernames() {
        Message sent = new Message(1L, 2L, "Hi", "body", MessageType.PLAYER, Instant.now());
        when(messageRepository.findBySenderUserIdOrderBySentAtDesc(1L)).thenReturn(List.of(sent));
        User recipient = new User("bob", "bob@example.com", "hash");
        recipient.setId(2L);
        when(userRepository.findAllById(any())).thenReturn(List.of(recipient));

        var views = service.sent(1L, "alice");

        assertThat(views).hasSize(1);
        assertThat(views.get(0).recipientUsername()).isEqualTo("bob");
    }

    @Test
    void unreadCountDelegatesToTheRepository() {
        when(messageRepository.countByRecipientUserIdAndReadAtIsNull(1L)).thenReturn(3L);

        assertThat(service.unreadCount(1L).count()).isEqualTo(3L);
    }

    @Test
    void markReadSetsReadAtOnlyOnceAndReturnsAView() {
        Message message = new Message(2L, 1L, "Hi", "body", MessageType.PLAYER, Instant.now());
        message.setId(5L);
        when(messageRepository.findByIdAndRecipientUserId(5L, 1L)).thenReturn(Optional.of(message));
        User sender = new User("bob", "bob@example.com", "hash");
        sender.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(sender));

        var view = service.markRead(1L, "alice", 5L);

        assertThat(view.senderUsername()).isEqualTo("bob");
        assertThat(view.recipientUsername()).isEqualTo("alice");
        assertThat(message.getReadAt()).isNotNull();
        verify(messageRepository).save(message);
    }

    @Test
    void markReadRejectsAMessageThatIsNotAddressedToTheCaller() {
        when(messageRepository.findByIdAndRecipientUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(1L, "alice", 5L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Message not found");
    }

    @Test
    void wipeAllDeletesEveryMessage() {
        service.wipeAll();

        verify(messageRepository).deleteAll();
    }
}
