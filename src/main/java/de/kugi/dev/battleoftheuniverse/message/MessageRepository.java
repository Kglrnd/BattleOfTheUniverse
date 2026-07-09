package de.kugi.dev.battleoftheuniverse.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRecipientUserIdOrderBySentAtDesc(Long recipientUserId);

    List<Message> findBySenderUserIdOrderBySentAtDesc(Long senderUserId);

    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    Optional<Message> findByIdAndRecipientUserId(Long id, Long recipientUserId);
}
