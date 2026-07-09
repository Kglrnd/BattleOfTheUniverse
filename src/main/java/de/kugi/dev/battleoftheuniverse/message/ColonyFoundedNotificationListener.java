package de.kugi.dev.battleoftheuniverse.message;

import de.kugi.dev.battleoftheuniverse.fleet.ColonyFounded;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class ColonyFoundedNotificationListener {

    private final MessageService messageService;

    public ColonyFoundedNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @ApplicationModuleListener
    void on(ColonyFounded event) {
        messageService.sendSystemMessage(event.ownerId(), "New colony founded",
                "Your colony ship has founded a new colony: " + event.planetName() + ".");
    }
}
