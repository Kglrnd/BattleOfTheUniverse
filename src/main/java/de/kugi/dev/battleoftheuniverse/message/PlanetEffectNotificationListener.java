package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.combat.PlanetDestroyed;
import de.kugi.dev.battleoftheuniverse.combat.PlanetInvaded;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlanetEffectNotificationListener {

    private final MessageService messageService;
    private final UserRepository userRepository;

    @ApplicationModuleListener
    void on(PlanetDestroyed event) {
        String attackerUsername = usernameOf(event.attackerId());
        String defenderUsername = usernameOf(event.defenderId());

        messageService.sendSystemMessage(event.attackerId(),
                "message.planetDestroyed.attacker.subject", new Object[] { event.planetName() },
                "message.planetDestroyed.attacker.body",
                new Object[] { event.planetName(), defenderUsername, event.planetCoordinates() });
        messageService.sendSystemMessage(event.defenderId(),
                "message.planetDestroyed.defender.subject", new Object[] { event.planetName() },
                "message.planetDestroyed.defender.body",
                new Object[] { event.planetName(), attackerUsername, event.planetCoordinates() });
    }

    @ApplicationModuleListener
    void on(PlanetInvaded event) {
        String attackerUsername = usernameOf(event.attackerId());
        String defenderUsername = usernameOf(event.defenderId());

        messageService.sendSystemMessage(event.attackerId(),
                "message.planetInvaded.attacker.subject", new Object[] { event.planetName() },
                "message.planetInvaded.attacker.body", new Object[] { event.planetName(), defenderUsername });
        messageService.sendSystemMessage(event.defenderId(),
                "message.planetInvaded.defender.subject", new Object[] { event.planetName() },
                "message.planetInvaded.defender.body", new Object[] { event.planetName(), attackerUsername });
    }

    private String usernameOf(Long userId) {
        return userRepository.findById(userId).map(User::getUsername).orElse("unknown");
    }
}
