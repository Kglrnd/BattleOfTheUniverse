package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.combat.BattleReport;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleNotificationListenerTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;

    private BattleNotificationListener listener;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        MessageService messageService = new MessageService(messageRepository, userRepository, messageSource);
        listener = new BattleNotificationListener(messageService, userRepository);
    }

    @Test
    void sendsAReportToBothTheAttackerAndTheDefenderWithLossesAndLoot() {
        User attacker = new User("alice", "alice@example.com", "hash");
        attacker.setId(1L);
        User defender = new User("bob", "bob@example.com", "hash");
        defender.setId(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(attacker));
        when(userRepository.findById(2L)).thenReturn(Optional.of(defender));

        BattleReport report = new BattleReport(1L, 2L, 20L, "Bob's Planet",
                Map.of("light_fighter", 5), Map.of("light_fighter", 1),
                Map.of("light_defense_tower", 3), Map.of("light_defense_tower", 3),
                Map.of(), Map.of(),
                new ResourceCost(100, 50, 0), Instant.now());

        listener.on(report);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, times(2)).save(captor.capture());

        Message toAttacker = captor.getAllValues().stream().filter(m -> m.getRecipientUserId().equals(1L)).findFirst().orElseThrow();
        assertThat(toAttacker.getBody()).contains("light_fighter: 5").contains("light_fighter: 1").contains("100");

        Message toDefender = captor.getAllValues().stream().filter(m -> m.getRecipientUserId().equals(2L)).findFirst().orElseThrow();
        assertThat(toDefender.getBody()).contains("light_defense_tower: 3");
    }

    @Test
    void fallsBackToUnknownWhenTheOpponentUsernameCannotBeResolved() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        BattleReport report = new BattleReport(1L, 2L, 20L, "Bob's Planet",
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                ResourceCost.ZERO, Instant.now());

        listener.on(report);

        verify(messageRepository, times(2)).save(any(Message.class));
    }
}
