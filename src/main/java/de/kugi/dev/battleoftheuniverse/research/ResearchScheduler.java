package de.kugi.dev.battleoftheuniverse.research;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResearchScheduler {

    private final ResearchService researchService;

    @Scheduled(fixedRate = 5000)
    public void completeDueResearch() {
        researchService.completeDueJobs();
    }
}
