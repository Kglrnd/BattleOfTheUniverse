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
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EspionageNotificationListener {

    private final MessageService messageService;
    private final UserRepository userRepository;

    @ApplicationModuleListener
    void on(EspionageResolved event) {
        if (event.success()) {
            messageService.sendSystemMessage(event.attackerId(), "Espionage report: " + event.targetPlanetName(),
                    reportBody(event.stationedShips(), event.resources()));
            return;
        }

        messageService.sendSystemMessage(event.attackerId(), "Espionage failed: " + event.targetPlanetName(),
                "Your probe failed to infiltrate " + event.targetPlanetName() + " and returned without gathering any intelligence.");

        String attackerUsername = userRepository.findById(event.attackerId()).map(User::getUsername).orElse("unknown");
        messageService.sendSystemMessage(event.defenderId(), "Espionage attempt detected",
                "A probe sent by " + attackerUsername + " attempted to spy on your planet " + event.targetPlanetName()
                        + " but was detected and repelled.");
    }

    private String reportBody(List<ShipQuantity> stationedShips, List<ResourceView> resources) {
        String shipsSection = stationedShips.isEmpty() ? "None"
                : stationedShips.stream().map(s -> s.shipKey() + ": " + s.quantity()).collect(Collectors.joining("\n"));
        String resourcesSection = resources.isEmpty() ? "None"
                : resources.stream().map(r -> r.displayName() + ": " + r.amount()).collect(Collectors.joining("\n"));
        return "Stationed fleet:\n" + shipsSection + "\n\nResources:\n" + resourcesSection;
    }
}
