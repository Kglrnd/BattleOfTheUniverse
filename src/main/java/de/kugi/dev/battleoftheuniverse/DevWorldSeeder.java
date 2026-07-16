package de.kugi.dev.battleoftheuniverse;

import de.kugi.dev.battleoftheuniverse.building.BuildingService;
import de.kugi.dev.battleoftheuniverse.catalog.ResourceKey;
import de.kugi.dev.battleoftheuniverse.fleet.FleetService;
import de.kugi.dev.battleoftheuniverse.planet.Planet;
import de.kugi.dev.battleoftheuniverse.planet.PlanetService;
import de.kugi.dev.battleoftheuniverse.research.ResearchService;
import de.kugi.dev.battleoftheuniverse.resource.ResourceService;
import de.kugi.dev.battleoftheuniverse.user.DevAccountsProperties;
import de.kugi.dev.battleoftheuniverse.user.User;
import de.kugi.dev.battleoftheuniverse.user.UserRepository;
import de.kugi.dev.battleoftheuniverse.user.UserService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Local-dev convenience run after {@link de.kugi.dev.battleoftheuniverse.user.DevAccountSeeder}:
 * seeds a handful of NPC-ish "enemy" accounts that already own several planets each (attack
 * targets for the seeded "player" account), and maxes out player's buildings/technologies plus
 * stocks a large fleet on their homeworld, so combat/fleet features can be exercised immediately
 * without hours of manual grinding. Lives in the base package (not inside any of the
 * {@code @ApplicationModule} packages) specifically so it can wire across module boundaries
 * that individual modules aren't allowed to cross themselves (e.g. {@code user} depends on
 * nothing) — this is bootstrap/dev-tooling glue code, not a module.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "game.dev", name = "seed-accounts", havingValue = "true", matchIfMissing = true)
@Order(2)
@RequiredArgsConstructor
public class DevWorldSeeder implements ApplicationRunner {

    private static final int MAX_LEVEL = 20;
    private static final int SHIP_QUANTITY = 500;
    private static final long HYDROGEN_STOCKPILE = 1_000_000;
    private static final int PLANETS_PER_ENEMY = 5;
    /**
     * Garrison on every defended enemy planet. {@code stockAllShips} stocks every catalog ship
     * type uniformly, including top-tier ones - even a small quantity of each stacks into a lot
     * of total defense power, so this is deliberately low to stay beatable by the bare-minimum
     * legal BOMBARD/INVADE escort (one Orbital Bomb/Invasion Unit + one Galaxy Class, 1800 attack
     * power total) as well as by a modest ATTACK fleet.
     */
    private static final int ENEMY_SHIP_QUANTITY = 5;
    private static final String ENEMY_PASSWORD = "enemy1234";

    private static final List<DevAccountsProperties.Account> ENEMIES = List.of(
            new DevAccountsProperties.Account("raider_vex", "raider_vex@battleoftheuniverse.local", ENEMY_PASSWORD),
            new DevAccountsProperties.Account("outlaw_korrin", "outlaw_korrin@battleoftheuniverse.local", ENEMY_PASSWORD),
            new DevAccountsProperties.Account("pirate_zara", "pirate_zara@battleoftheuniverse.local", ENEMY_PASSWORD)
    );

    private final UserRepository userRepository;
    private final UserService userService;
    private final PlanetService planetService;
    private final BuildingService buildingService;
    private final ResearchService researchService;
    private final FleetService fleetService;
    private final ResourceService resourceService;
    private final DevAccountsProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        for (DevAccountsProperties.Account enemy : ENEMIES) {
            seedEnemy(enemy);
        }
        boostPlayer(properties.player());
    }

    private void seedEnemy(DevAccountsProperties.Account account) {
        boolean alreadyExists = userRepository.existsByUsername(account.username());
        if (!alreadyExists) {
            userService.register(account.username(), account.email(), account.password());
        }
        Long ownerId = userRepository.findByUsername(account.username()).orElseThrow().getId();

        Planet home = awaitHomePlanet(ownerId);
        if (home == null || alreadyExists) {
            // Either registration failed to produce a homeworld in time, or this account
            // (and its colonies) already existed from a previous run - nothing more to do.
            return;
        }
        for (int i = 1; i < PLANETS_PER_ENEMY; i++) {
            planetService.createColonyPlanet(ownerId, account.username() + " Colony " + i);
        }

        // Stock a defensive garrison on every one of this enemy's planets except the very last
        // colony, which is deliberately left undefended - a guaranteed easy target for testing
        // the undefended BOMBARD/INVADE path, alongside the defended homeworld/other colonies
        // for testing the "escort dies in combat" path.
        List<Planet> planets = planetService.listMine(ownerId);
        for (int i = 0; i < planets.size(); i++) {
            if (i == planets.size() - 1) {
                continue;
            }
            fleetService.stockAllShips(planets.get(i).getId(), ENEMY_SHIP_QUANTITY);
        }
    }

    private void boostPlayer(DevAccountsProperties.@Nullable Account account) {
        if (account == null) {
            return;
        }
        Long playerId = userRepository.findByUsername(account.username()).map(User::getId).orElse(null);
        if (playerId == null) {
            return;
        }
        Planet home = awaitHomePlanet(playerId);
        if (home == null) {
            return;
        }
        buildingService.maxAllBuildings(home.getId(), MAX_LEVEL);
        researchService.maxAllTechnologies(playerId, MAX_LEVEL);
        fleetService.stockAllShips(home.getId(), SHIP_QUANTITY);
        resourceService.credit(home.getId(), ResourceKey.HYDROGEN, HYDROGEN_STOCKPILE);
    }

    /**
     * The homeworld is created asynchronously off the {@code UserRegistered} event
     * (see {@code planet.UserRegistrationListener}), so it may not exist the instant
     * registration returns. Dev-startup-only polling to bridge that gap.
     */
    private @Nullable Planet awaitHomePlanet(Long ownerId) {
        for (int attempt = 0; attempt < 50; attempt++) {
            try {
                return planetService.getHome(ownerId);
            } catch (ResponseStatusException _) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }
}
