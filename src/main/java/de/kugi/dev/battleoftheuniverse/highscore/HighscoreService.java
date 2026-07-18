package de.kugi.dev.battleoftheuniverse.highscore;

import de.kugi.dev.battleoftheuniverse.building.PlanetBuildingRepository;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.defense.TowerRepository;
import de.kugi.dev.battleoftheuniverse.fleet.ShipRepository;
import de.kugi.dev.battleoftheuniverse.highscore.dto.HighscoreEntryDto;
import de.kugi.dev.battleoftheuniverse.highscore.dto.HighscoreResponseDto;
import de.kugi.dev.battleoftheuniverse.planet.PlanetRepository;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ranks players by a score derived from what they've built: each building level, ship and
 * defense tower contributes its catalog {@code points} value. Colony ships and transports are
 * worth 0 points in the catalog data, so they don't contribute. Scores are computed from three
 * grouped-sum queries (one per building/ship/tower) rather than per-planet-per-user loops, so
 * this stays cheap regardless of player or planet count.
 */
@Service
@RequiredArgsConstructor
public class HighscoreService {

    private static final int TOP_LIMIT = 100;

    private final UserRepository userRepository;
    private final PlanetRepository planetRepository;
    private final PlanetBuildingRepository planetBuildingRepository;
    private final ShipRepository shipRepository;
    private final TowerRepository towerRepository;
    private final CatalogService catalogService;

    public HighscoreResponseDto get(Long currentUserId) {
        List<RankedUser> ranked = rankedScores();

        List<HighscoreEntryDto> top = ranked.stream()
                .limit(TOP_LIMIT)
                .map(r -> new HighscoreEntryDto(r.rank(), r.userId(), r.username(), r.score()))
                .toList();

        HighscoreEntryDto me = ranked.stream()
                .filter(r -> r.userId().equals(currentUserId))
                .findFirst()
                .map(r -> new HighscoreEntryDto(r.rank(), r.userId(), r.username(), r.score()))
                .orElse(null);

        return new HighscoreResponseDto(top, me);
    }

    private List<RankedUser> rankedScores() {
        Set<Long> ownersWithPlanets = new HashSet<>(planetRepository.findDistinctOwnerIds());
        Map<Long, Long> scoreByOwnerId = new HashMap<>();
        for (var row : planetBuildingRepository.sumLevelsByOwnerAndBuildingKey()) {
            scoreByOwnerId.merge(row.getOwnerId(), row.getTotal() * catalogService.building(row.getKey()).points(), Long::sum);
        }
        for (var row : shipRepository.sumQuantitiesByOwnerAndShipKey()) {
            scoreByOwnerId.merge(row.getOwnerId(), row.getTotal() * catalogService.ship(row.getKey()).points(), Long::sum);
        }
        for (var row : towerRepository.sumQuantitiesByOwnerAndTowerKey()) {
            scoreByOwnerId.merge(row.getOwnerId(), row.getTotal() * catalogService.defense(row.getKey()).points(), Long::sum);
        }

        List<ScoredUser> scored = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            if (!ownersWithPlanets.contains(user.getId())) {
                continue;
            }
            scored.add(new ScoredUser(user.getId(), user.getUsername(), scoreByOwnerId.getOrDefault(user.getId(), 0L)));
        }
        scored.sort(Comparator.comparingLong(ScoredUser::score).reversed());

        List<RankedUser> ranked = new ArrayList<>(scored.size());
        int rank = 0;
        long previousScore = Long.MIN_VALUE;
        for (int i = 0; i < scored.size(); i++) {
            ScoredUser current = scored.get(i);
            if (i == 0 || current.score() != previousScore) {
                rank = i + 1;
                previousScore = current.score();
            }
            ranked.add(new RankedUser(rank, current.userId(), current.username(), current.score()));
        }
        return ranked;
    }

    private record ScoredUser(Long userId, String username, long score) {
    }

    private record RankedUser(int rank, Long userId, String username, long score) {
    }
}
