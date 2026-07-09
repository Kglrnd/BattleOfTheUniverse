package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.message.dto.MessageView;
import de.kugi.dev.battleoftheuniverse.message.dto.SendMessageRequest;
import de.kugi.dev.battleoftheuniverse.message.dto.UnreadCountView;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final String SYSTEM_SENDER_LABEL = "System";

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public MessageView send(Long senderUserId, String senderUsername, SendMessageRequest request) {
        User recipient = userRepository.findByUsername(request.recipientUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"));
        if (recipient.getId().equals(senderUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send a message to yourself");
        }

        Message message = messageRepository.save(new Message(senderUserId, recipient.getId(),
                request.subject(), request.body(), MessageType.PLAYER, Instant.now()));
        return new MessageView(message.getId(), senderUsername, recipient.getUsername(),
                message.getSubject(), message.getBody(), message.getType(), message.getSentAt(), message.getReadAt());
    }

    @Transactional
    public void sendSystemMessage(Long recipientUserId, String subject, String body) {
        messageRepository.save(new Message(null, recipientUserId, subject, body, MessageType.SYSTEM, Instant.now()));
    }

    /** Admin-triggered game reset: clears every message, game-wide. */
    @Transactional
    public void wipeAll() {
        messageRepository.deleteAll();
    }

    public List<MessageView> inbox(Long userId, String username) {
        List<Message> messages = messageRepository.findByRecipientUserIdOrderBySentAtDesc(userId);
        Map<Long, String> usernamesById = resolveUsernames(messages, Message::getSenderUserId);
        return messages.stream()
                .map(m -> new MessageView(m.getId(),
                        m.getSenderUserId() == null ? SYSTEM_SENDER_LABEL : usernamesById.getOrDefault(m.getSenderUserId(), "unknown"),
                        username, m.getSubject(), m.getBody(), m.getType(), m.getSentAt(), m.getReadAt()))
                .toList();
    }

    public List<MessageView> sent(Long userId, String username) {
        List<Message> messages = messageRepository.findBySenderUserIdOrderBySentAtDesc(userId);
        Map<Long, String> usernamesById = resolveUsernames(messages, Message::getRecipientUserId);
        return messages.stream()
                .map(m -> new MessageView(m.getId(), username,
                        usernamesById.getOrDefault(m.getRecipientUserId(), "unknown"),
                        m.getSubject(), m.getBody(), m.getType(), m.getSentAt(), m.getReadAt()))
                .toList();
    }

    public UnreadCountView unreadCount(Long userId) {
        return new UnreadCountView(messageRepository.countByRecipientUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public MessageView markRead(Long userId, Long messageId) {
        Message message = messageRepository.findByIdAndRecipientUserId(messageId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (message.getReadAt() == null) {
            message.setReadAt(Instant.now());
            messageRepository.save(message);
        }

        String senderUsername = message.getSenderUserId() == null ? SYSTEM_SENDER_LABEL
                : userRepository.findById(message.getSenderUserId()).map(User::getUsername).orElse("unknown");
        return new MessageView(message.getId(), senderUsername, userRepository.findById(userId).map(User::getUsername).orElse("unknown"),
                message.getSubject(), message.getBody(), message.getType(), message.getSentAt(), message.getReadAt());
    }

    private Map<Long, String> resolveUsernames(List<Message> messages, java.util.function.Function<Message, Long> idExtractor) {
        Set<Long> ids = messages.stream().map(idExtractor).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, User::getUsername));
    }
}
