package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.catalog.ResourceCost;
import de.kugi.dev.battleoftheuniverse.combat.BattleReport;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BattleNotificationListener {

    private final MessageService messageService;
    private final UserRepository userRepository;

    @ApplicationModuleListener
    void on(BattleReport report) {
        Locale attackerLocale = messageService.resolveLocale(report.attackerId());
        String defenderUsername = userRepository.findById(report.defenderId()).map(User::getUsername).orElse("unknown");
        String attackerBody = messageService.translate(attackerLocale, "message.battle.attacker.intro", defenderUsername, report.defenderPlanetName())
                + "\n\n" + messageService.translate(attackerLocale, "message.battle.yourFleet") + "\n" + formatLosses(attackerLocale, report.attackerShips())
                + "\n\n" + messageService.translate(attackerLocale, "message.battle.enemyTowers") + "\n" + formatLosses(attackerLocale, report.defenderTowers())
                + "\n\n" + messageService.translate(attackerLocale, "message.battle.enemyFleet") + "\n" + formatLosses(attackerLocale, report.defenderShips())
                + "\n\n" + messageService.translate(attackerLocale, "message.battle.enemyTowerLosses") + "\n" + formatLosses(attackerLocale, report.defenderTowerLosses())
                + "\n\n" + messageService.translate(attackerLocale, "message.battle.enemyFleetLosses") + "\n" + formatLosses(attackerLocale, report.defenderShipLosses())
                + "\n\n" + messageService.translate(attackerLocale, "message.battle.yourLosses") + "\n" + formatLosses(attackerLocale, report.attackerLosses())
                + "\n\n" + messageService.translate(attackerLocale, "message.battle.resourcesLooted") + "\n" + formatLoot(attackerLocale, report.resourcesLooted());
        messageService.sendSystemMessage(report.attackerId(),
                "message.battle.attacker.subject", new Object[] { report.defenderPlanetName() },
                attackerBody);

        Locale defenderLocale = messageService.resolveLocale(report.defenderId());
        String attackerUsername = userRepository.findById(report.attackerId()).map(User::getUsername).orElse("unknown");
        String defenderBody = messageService.translate(defenderLocale, "message.battle.defender.intro", attackerUsername, report.defenderPlanetName())
                + "\n\n" + messageService.translate(defenderLocale, "message.battle.enemyFleet") + "\n" + formatLosses(defenderLocale, report.attackerShips())
                + "\n\n" + messageService.translate(defenderLocale, "message.battle.yourTowers") + "\n" + formatLosses(defenderLocale, report.defenderTowers())
                + "\n\n" + messageService.translate(defenderLocale, "message.battle.yourFleet") + "\n" + formatLosses(defenderLocale, report.defenderShips())
                + "\n\n" + messageService.translate(defenderLocale, "message.battle.yourTowerLosses") + "\n" + formatLosses(defenderLocale, report.defenderTowerLosses())
                + "\n\n" + messageService.translate(defenderLocale, "message.battle.yourFleetLosses") + "\n" + formatLosses(defenderLocale, report.defenderShipLosses())
                + "\n\n" + messageService.translate(defenderLocale, "message.battle.enemyLosses") + "\n" + formatLosses(defenderLocale, report.attackerLosses())
                + "\n\n" + messageService.translate(defenderLocale, "message.battle.resourcesLootedFromYou") + "\n" + formatLoot(defenderLocale, report.resourcesLooted());
        messageService.sendSystemMessage(report.defenderId(),
                "message.battle.defender.subject", new Object[] { report.defenderPlanetName() },
                defenderBody);
    }

    private String formatLosses(Locale locale, Map<String, Integer> losses) {
        return losses.isEmpty() ? messageService.translate(locale, "message.common.none")
                : losses.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("\n"));
    }

    private String formatLoot(Locale locale, ResourceCost loot) {
        if (loot.metal() <= 0 && loot.crystal() <= 0 && loot.deuterium() <= 0) {
            return messageService.translate(locale, "message.common.none");
        }
        return messageService.translate(locale, "message.resource.metal") + ": " + loot.metal()
                + ", " + messageService.translate(locale, "message.resource.crystal") + ": " + loot.crystal()
                + ", " + messageService.translate(locale, "message.resource.deuterium") + ": " + loot.deuterium();
    }
}
