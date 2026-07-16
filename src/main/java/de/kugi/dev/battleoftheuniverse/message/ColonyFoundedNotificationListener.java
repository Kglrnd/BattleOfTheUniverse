package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.fleet.ColonyFounded;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ColonyFoundedNotificationListener {

    private final MessageService messageService;

    @ApplicationModuleListener
    void on(ColonyFounded event) {
        Locale locale = messageService.resolveLocale(event.ownerId());
        String researchEfficiency = String.format(Locale.ROOT, "%.2f%%", event.researchEfficiency());
        String body = messageService.translate(locale, "message.colonyFounded.intro", event.planetName())
                + "\n\n" + messageService.translate(locale, "message.colonyFounded.researchEfficiency") + " " + researchEfficiency
                + "\n\n" + messageService.translate(locale, "message.colonyFounded.productionEfficiency") + "\n"
                + productionEfficiencyLines(event.productionEfficiencies());
        messageService.sendSystemMessage(event.ownerId(), "message.colonyFounded.subject", new Object[0], body);
    }

    private String productionEfficiencyLines(Map<String, Double> productionEfficiencies) {
        return productionEfficiencies.entrySet().stream()
                .map(e -> e.getKey() + ": " + String.format(Locale.ROOT, "%.2f%%", e.getValue()))
                .collect(Collectors.joining("\n"));
    }
}
