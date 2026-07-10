package de.kugi.dev.battleoftheuniverse.highscore;

import de.kugi.dev.battleoftheuniverse.building.PlanetBuildingRepository;
import de.kugi.dev.battleoftheuniverse.catalog.CatalogService;
import de.kugi.dev.battleoftheuniverse.defense.TowerRepository;
import de.kugi.dev.battleoftheuniverse.fleet.ShipRepository;
import de.kugi.dev.battleoftheuniverse.highscore.dto.HighscoreEntryDto;
import de.kugi.dev.battleoftheuniverse.highscore.dto.HighscoreResponseDto;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetRepository;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ranks players by a score derived from what they've built: each building level, ship and
 * defense tower contributes its catalog {@code points} value. Colony ships and transports are
 * worth 0 points in the catalog data, so they don't contribute.
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
        List<ScoredUser> scored = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            List<Planet> planets = planetRepository.findByOwnerId(user.getId());
            if (planets.isEmpty()) {
                continue;
            }
            long score = planets.stream().mapToLong(planet -> scoreForPlanet(planet.getId())).sum();
            scored.add(new ScoredUser(user.getId(), user.getUsername(), score));
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

    private long scoreForPlanet(Long planetId) {
        long buildingPoints = planetBuildingRepository.findByPlanetId(planetId).stream()
                .mapToLong(pb -> (long) pb.getLevel() * catalogService.building(pb.getBuildingKey()).points())
                .sum();
        long shipPoints = shipRepository.findByPlanetId(planetId).stream()
                .mapToLong(ship -> (long) ship.getQuantity() * catalogService.ship(ship.getShipKey()).points())
                .sum();
        long towerPoints = towerRepository.findByPlanetId(planetId).stream()
                .mapToLong(tower -> (long) tower.getQuantity() * catalogService.defense(tower.getTowerKey()).points())
                .sum();
        return buildingPoints + shipPoints + towerPoints;
    }

    private record ScoredUser(Long userId, String username, long score) {
    }

    private record RankedUser(int rank, Long userId, String username, long score) {
    }
}
