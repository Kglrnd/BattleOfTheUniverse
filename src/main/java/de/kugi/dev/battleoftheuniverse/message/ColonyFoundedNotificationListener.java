package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.fleet.ColonyFounded;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ColonyFoundedNotificationListener {

    private final MessageService messageService;

    @ApplicationModuleListener
    void on(ColonyFounded event) {
        messageService.sendSystemMessage(event.ownerId(), "New colony founded",
                "Your colony ship has founded a new colony: " + event.planetName() + ".\n\n"
                        + "Research efficiency: " + String.format(Locale.ROOT, "%.2f%%", event.researchEfficiency()) + ".");
    }
}
