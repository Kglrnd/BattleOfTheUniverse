package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.combat.BattleReport;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BattleNotificationListener {

    private final MessageService messageService;
    private final UserRepository userRepository;

    @ApplicationModuleListener
    void on(BattleReport report) {
        String defenderUsername = userRepository.findById(report.defenderId()).map(User::getUsername).orElse("unknown");
        messageService.sendSystemMessage(report.attackerId(), "Battle report: " + report.defenderPlanetName(),
                "Your fleet attacked " + defenderUsername + "'s planet " + report.defenderPlanetName() + ".\n\n"
                        + "Enemy tower losses:\n" + formatLosses(report.defenderTowerLosses())
                        + "\n\nEnemy fleet losses:\n" + formatLosses(report.defenderShipLosses())
                        + "\n\nYour losses:\n" + formatLosses(report.attackerLosses()));

        String attackerUsername = userRepository.findById(report.attackerId()).map(User::getUsername).orElse("unknown");
        messageService.sendSystemMessage(report.defenderId(), "Your planet was attacked: " + report.defenderPlanetName(),
                attackerUsername + " attacked your planet " + report.defenderPlanetName() + ".\n\n"
                        + "Your tower losses:\n" + formatLosses(report.defenderTowerLosses())
                        + "\n\nYour fleet losses:\n" + formatLosses(report.defenderShipLosses())
                        + "\n\nEnemy losses:\n" + formatLosses(report.attackerLosses()));
    }

    private String formatLosses(Map<String, Integer> losses) {
        return losses.isEmpty() ? "None"
                : losses.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("\n"));
    }
}
