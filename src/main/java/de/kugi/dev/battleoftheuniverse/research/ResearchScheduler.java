package de.kugi.dev.battleoftheuniverse.research;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ResearchScheduler {

    private final ResearchService researchService;

    public ResearchScheduler(ResearchService researchService) {
        this.researchService = researchService;
    }

    @Scheduled(fixedRate = 5000)
    public void completeDueResearch() {
        researchService.completeDueJobs();
    }
}
