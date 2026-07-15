package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.fleet.EspionageResolved;
import de.kugi.dev.battleoftheuniverse.fleet.dto.ShipQuantity;
import de.kugi.dev.battleoftheuniverse.resource.dto.ResourceView;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EspionageNotificationListener {

    private final MessageService messageService;
    private final UserRepository userRepository;

    @ApplicationModuleListener
    void on(EspionageResolved event) {
        if (event.success()) {
            Locale locale = messageService.resolveLocale(event.attackerId());
            messageService.sendSystemMessage(event.attackerId(),
                    "message.espionage.report.subject", new Object[] { event.targetPlanetName() },
                    reportBody(locale, event.stationedShips(), event.resources()));
            return;
        }

        messageService.sendSystemMessage(event.attackerId(),
                "message.espionage.failed.subject", new Object[] { event.targetPlanetName() },
                "message.espionage.failed.body", new Object[] { event.targetPlanetName() });

        String attackerUsername = userRepository.findById(event.attackerId()).map(User::getUsername).orElse("unknown");
        messageService.sendSystemMessage(event.defenderId(),
                "message.espionage.detected.subject", new Object[0],
                "message.espionage.detected.body", new Object[] { attackerUsername, event.targetPlanetName() });
    }

    private String reportBody(Locale locale, List<ShipQuantity> stationedShips, List<ResourceView> resources) {
        String none = messageService.translate(locale, "message.common.none");
        String shipsSection = stationedShips.isEmpty() ? none
                : stationedShips.stream().map(s -> s.shipKey() + ": " + s.quantity()).collect(Collectors.joining("\n"));
        String resourcesSection = resources.isEmpty() ? none
                : resources.stream().map(r -> r.displayName() + ": " + r.amount()).collect(Collectors.joining("\n"));
        return messageService.translate(locale, "message.espionage.stationedFleet") + "\n" + shipsSection
                + "\n\n" + messageService.translate(locale, "message.espionage.resources") + "\n" + resourcesSection;
    }
}
