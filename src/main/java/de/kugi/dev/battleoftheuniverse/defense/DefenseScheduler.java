package de.kugi.dev.battleoftheuniverse.defense;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefenseScheduler {

    private final DefenseService defenseService;

    @Scheduled(fixedRate = 5000)
    public void completeDueBuilds() {
        defenseService.completeDueJobs();
    }
}
