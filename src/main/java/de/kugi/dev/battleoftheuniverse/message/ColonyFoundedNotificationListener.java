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
        String efficiency = String.format(Locale.ROOT, "%.2f%%", event.researchEfficiency());
        messageService.sendSystemMessage(event.ownerId(),
                "message.colonyFounded.subject", new Object[0],
                "message.colonyFounded.body", new Object[] { event.planetName(), efficiency });
    }
}
